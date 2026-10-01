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
}
