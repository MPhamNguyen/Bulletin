package com.jdrms.bulletin.app.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ThemePreferenceStore {
    val themePreference: StateFlow<ThemePreference>

    fun setThemePreference(preference: ThemePreference)
}

class InMemoryThemePreferenceStore(
    initialPreference: ThemePreference = ThemePreference.SYSTEM
) : ThemePreferenceStore {
    private val _themePreference = MutableStateFlow(initialPreference)
    override val themePreference: StateFlow<ThemePreference> = _themePreference.asStateFlow()

    override fun setThemePreference(preference: ThemePreference) {
        _themePreference.value = preference
    }
}
