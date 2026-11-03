package com.jdrms.bulletin.app.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.designsystem.theme.BulletinThemeColor
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ThemeViewModel(
    private val themePreferenceStore: ThemePreferenceStore
) : ViewModel() {
    private val _themePreference = MutableStateFlow(ThemePreference.SYSTEM)
    val themePreference: StateFlow<ThemePreference> = _themePreference.asStateFlow()
    private val _themeColor = MutableStateFlow(BulletinThemeColor.CLASSIC)
    val themeColor: StateFlow<BulletinThemeColor> = _themeColor.asStateFlow()
    private var currentUserId: UserId? = null

    init {
        viewModelScope.launch {
            val initial = themePreferenceStore.lastAppliedPreference()
            val initialThemeColor = themePreferenceStore.lastAppliedThemeColor()
            if (currentUserId == null) {
                _themePreference.update { initial }
                _themeColor.update { initialThemeColor }
            }
        }
    }

    fun setAccount(userId: UserId?) {
        if (currentUserId == userId) return

        currentUserId = userId
        viewModelScope.launch {
            val resolvedPreference = userId?.let { themePreferenceStore.preferenceFor(it) }
                ?: themePreferenceStore.lastAppliedPreference()
            val resolvedThemeColor = userId?.let { themePreferenceStore.themeColorFor(it) }
                ?: themePreferenceStore.lastAppliedThemeColor()
            themePreferenceStore.setLastAppliedPreference(resolvedPreference)
            themePreferenceStore.setLastAppliedThemeColor(resolvedThemeColor)
            _themePreference.update { resolvedPreference }
            _themeColor.update { resolvedThemeColor }
        }
    }

    fun setThemePreference(preference: ThemePreference) {
        val userId = currentUserId ?: return
        _themePreference.update { preference }
        viewModelScope.launch {
            themePreferenceStore.setPreference(userId, preference)
        }
    }

    fun setThemeColor(themeColor: BulletinThemeColor) {
        val userId = currentUserId ?: return
        _themeColor.update { themeColor }
        viewModelScope.launch {
            themePreferenceStore.setThemeColor(userId, themeColor)
        }
    }
}
