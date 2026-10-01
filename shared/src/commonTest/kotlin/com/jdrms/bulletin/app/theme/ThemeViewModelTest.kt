package com.jdrms.bulletin.app.theme

import com.jdrms.bulletin.domain.profile.domain.model.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeViewModelTest {
    @Test
    fun defaultsToSystemPreference() {
        val viewModel = ThemeViewModel(InMemoryThemePreferenceStore())

        viewModel.setAccount(UserId("user-1"))

        assertEquals(ThemePreference.SYSTEM, viewModel.themePreference.value)
    }

    @Test
    fun settingPreferenceUpdatesState() {
        val viewModel = ThemeViewModel(InMemoryThemePreferenceStore())

        viewModel.setAccount(UserId("user-1"))
        viewModel.setThemePreference(ThemePreference.DARK)

        assertEquals(ThemePreference.DARK, viewModel.themePreference.value)
    }

    @Test
    fun preferenceIsRestoredAcrossViewModelRecreation() {
        val store = InMemoryThemePreferenceStore()
        val firstViewModel = ThemeViewModel(store)
        firstViewModel.setAccount(UserId("user-1"))
        firstViewModel.setThemePreference(ThemePreference.LIGHT)

        val recreatedViewModel = ThemeViewModel(store)
        recreatedViewModel.setAccount(UserId("user-1"))

        assertEquals(ThemePreference.LIGHT, recreatedViewModel.themePreference.value)
    }

    @Test
    fun preferencesAreIsolatedPerAccount() {
        val store = InMemoryThemePreferenceStore()
        val viewModel = ThemeViewModel(store)

        viewModel.setAccount(UserId("user-1"))
        viewModel.setThemePreference(ThemePreference.DARK)
        viewModel.setAccount(UserId("user-2"))

        assertEquals(ThemePreference.SYSTEM, viewModel.themePreference.value)

        viewModel.setThemePreference(ThemePreference.LIGHT)
        viewModel.setAccount(UserId("user-1"))

        assertEquals(ThemePreference.DARK, viewModel.themePreference.value)
    }

    @Test
    fun anonymousPreferenceChangesDoNotPersist() {
        val store = InMemoryThemePreferenceStore()
        val viewModel = ThemeViewModel(store)

        viewModel.setThemePreference(ThemePreference.DARK)
        viewModel.setAccount(UserId("user-1"))

        assertEquals(ThemePreference.SYSTEM, viewModel.themePreference.value)
    }
}
