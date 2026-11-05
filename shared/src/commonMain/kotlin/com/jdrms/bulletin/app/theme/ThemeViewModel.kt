package com.jdrms.bulletin.app.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.designsystem.theme.BulletinThemeColor
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ThemeViewModel(
    private val themePreferenceStore: ThemePreferenceStores
) : ViewModel() {
    private val _themePreference = MutableStateFlow(ThemePreference.SYSTEM)
    val themePreference: StateFlow<ThemePreference> = _themePreference.asStateFlow()
    private val _themeColor = MutableStateFlow(BulletinThemeColor.CLASSIC)
    val themeColor: StateFlow<BulletinThemeColor> = _themeColor.asStateFlow()
    private val _baseColor = MutableStateFlow(BulletinThemeColor.BASE)
    val baseColor: StateFlow<BulletinThemeColor> = _baseColor.asStateFlow()
    private var currentUserId: UserId? = null
    private var accountLoadJob: Job? = null

    init {
        viewModelScope.launch {
            val initial = themePreferenceStore.lastAppliedPreference()
            val initialThemeColor = themePreferenceStore.lastAppliedThemeColor()
            val initialBaseColor = themePreferenceStore.lastAppliedBaseColor()
            if (currentUserId == null) {
                _themePreference.update { initial }
                _themeColor.update { initialThemeColor }
                _baseColor.update { initialBaseColor }
            }
        }
    }

    fun setAccount(userId: UserId?) {
        if (currentUserId == userId) return

        currentUserId = userId
        accountLoadJob?.cancel()
        accountLoadJob = viewModelScope.launch {
            val resolvedPreference = userId?.let { themePreferenceStore.preferenceFor(it) }
                ?: themePreferenceStore.lastAppliedPreference()
            val resolvedThemeColor = userId?.let { themePreferenceStore.themeColorFor(it) }
                ?: themePreferenceStore.lastAppliedThemeColor()
            val resolvedBaseColor = userId?.let { themePreferenceStore.baseColorFor(it) }
                ?: themePreferenceStore.lastAppliedBaseColor()
            if (currentUserId != userId) return@launch
            themePreferenceStore.setLastAppliedPreference(resolvedPreference)
            themePreferenceStore.setLastAppliedThemeColors(resolvedBaseColor, resolvedThemeColor)
            if (currentUserId != userId) return@launch
            _themePreference.update { resolvedPreference }
            _themeColor.update { resolvedThemeColor }
            _baseColor.update { resolvedBaseColor }
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
            themePreferenceStore.setThemeColors(userId, _baseColor.value, themeColor)
        }
    }

    fun setThemeColors(baseColor: BulletinThemeColor, themeColor: BulletinThemeColor) {
        val userId = currentUserId ?: return
        _baseColor.update { baseColor }
        _themeColor.update { themeColor }
        viewModelScope.launch {
            themePreferenceStore.setThemeColors(userId, baseColor, themeColor)
        }
    }
}
