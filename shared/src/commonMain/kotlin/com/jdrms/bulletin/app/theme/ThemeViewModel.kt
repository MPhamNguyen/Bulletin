package com.jdrms.bulletin.app.theme

import androidx.lifecycle.ViewModel
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeViewModel(
    private val themePreferenceStore: ThemePreferenceStore
) : ViewModel() {
    private val _themePreference = MutableStateFlow(ThemePreference.SYSTEM)
    val themePreference: StateFlow<ThemePreference> = _themePreference.asStateFlow()
    private var currentUserId: UserId? = null

    fun setAccount(userId: UserId?) {
        if (currentUserId == userId) return

        currentUserId = userId
        _themePreference.value = userId?.let(themePreferenceStore::preferenceFor) ?: ThemePreference.SYSTEM
    }

    fun setThemePreference(preference: ThemePreference) {
        val userId = currentUserId ?: return
        themePreferenceStore.setPreference(userId, preference)
        _themePreference.value = preference
    }
}
