package com.jdrms.bulletin.app.theme

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

class ThemeViewModel(
    private val themePreferenceStore: ThemePreferenceStore
) : ViewModel() {
    val themePreference: StateFlow<ThemePreference> = themePreferenceStore.themePreference

    fun setThemePreference(preference: ThemePreference) {
        themePreferenceStore.setThemePreference(preference)
    }
}
