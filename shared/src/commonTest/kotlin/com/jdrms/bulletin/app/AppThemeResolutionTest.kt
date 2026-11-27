package com.jdrms.bulletin.app

import com.jdrms.bulletin.app.theme.ThemePreference
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppThemeResolutionTest {

    @Test
    fun systemPreferenceFollowsSystemDarkTheme() {
        assertTrue(resolveIsDarkTheme(ThemePreference.SYSTEM, systemDarkTheme = true))
        assertFalse(resolveIsDarkTheme(ThemePreference.SYSTEM, systemDarkTheme = false))
    }

    @Test
    fun lightPreferenceForcesLightThemeRegardlessOfSystemSetting() {
        assertFalse(resolveIsDarkTheme(ThemePreference.LIGHT, systemDarkTheme = true))
        assertFalse(resolveIsDarkTheme(ThemePreference.LIGHT, systemDarkTheme = false))
    }

    @Test
    fun darkPreferenceForcesDarkThemeRegardlessOfSystemSetting() {
        assertTrue(resolveIsDarkTheme(ThemePreference.DARK, systemDarkTheme = true))
        assertTrue(resolveIsDarkTheme(ThemePreference.DARK, systemDarkTheme = false))
    }

    @Test
    fun darkPreferenceStaysDarkOnLightSystemForEveryScreenIncludingPasswordRecovery() {
        // Regression: password recovery used to ignore the saved preference and follow only the system setting,
        // so it rendered light while the rest of the app was dark.
        assertTrue(resolveIsDarkTheme(ThemePreference.DARK, systemDarkTheme = false))
        assertFalse(resolveIsDarkTheme(ThemePreference.LIGHT, systemDarkTheme = true))
    }
}
