package com.jdrms.bulletin.app.theme

import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeViewModelTest {
    @Test
    fun defaultsToSystemPreference() {
        val viewModel = ThemeViewModel(InMemoryThemePreferenceStore())

        assertEquals(ThemePreference.SYSTEM, viewModel.themePreference.value)
    }

    @Test
    fun settingPreferenceUpdatesState() {
        val viewModel = ThemeViewModel(InMemoryThemePreferenceStore())

        viewModel.setThemePreference(ThemePreference.DARK)

        assertEquals(ThemePreference.DARK, viewModel.themePreference.value)
    }

    @Test
    fun preferenceIsSharedAcrossViewModelRecreation() {
        val store = InMemoryThemePreferenceStore()
        val firstViewModel = ThemeViewModel(store)
        firstViewModel.setThemePreference(ThemePreference.LIGHT)

        val recreatedViewModel = ThemeViewModel(store)

        assertEquals(ThemePreference.LIGHT, recreatedViewModel.themePreference.value)
    }
}
