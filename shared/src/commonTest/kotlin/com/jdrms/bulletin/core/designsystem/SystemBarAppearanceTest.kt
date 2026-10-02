package com.jdrms.bulletin.core.designsystem

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SystemBarAppearanceTest {

    @Test
    fun darkThemeRequiresLightSystemBarIcons() {
        val appearance = resolveSystemBarAppearance(isDarkTheme = true)

        assertFalse(
            actual = appearance.isAppearanceLightStatusBars,
            message = "Status bar icons should be light on dark background"
        )
        assertFalse(
            actual = appearance.isAppearanceLightNavigationBars,
            message = "Navigation bar icons should be light on dark background"
        )
    }

    @Test
    fun lightThemeRequiresDarkSystemBarIcons() {
        val appearance = resolveSystemBarAppearance(isDarkTheme = false)

        assertTrue(
            actual = appearance.isAppearanceLightStatusBars,
            message = "Status bar icons should be dark on light background"
        )
        assertTrue(
            actual = appearance.isAppearanceLightNavigationBars,
            message = "Navigation bar icons should be dark on light background"
        )
    }
}
