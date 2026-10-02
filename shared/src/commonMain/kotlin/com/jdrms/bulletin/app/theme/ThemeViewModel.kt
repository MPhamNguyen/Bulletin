package com.jdrms.bulletin.app.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private var currentUserId: UserId? = null

    init {
        viewModelScope.launch {
            val initial = themePreferenceStore.lastAppliedPreference()
            if (currentUserId == null) {
                _themePreference.update { initial }
            }
        }
    }

    fun setAccount(userId: UserId?) {
        if (currentUserId == userId) return

        currentUserId = userId
        viewModelScope.launch {
            val resolvedPreference = userId?.let { themePreferenceStore.preferenceFor(it) }
                ?: themePreferenceStore.lastAppliedPreference()
            themePreferenceStore.setLastAppliedPreference(resolvedPreference)
            _themePreference.update { resolvedPreference }
        }
    }

    fun setThemePreference(preference: ThemePreference) {
        val userId = currentUserId ?: return
        _themePreference.update { preference }
        viewModelScope.launch {
            themePreferenceStore.setPreference(userId, preference)
        }
    }
}
