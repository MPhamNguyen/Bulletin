package com.jdrms.bulletin.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class BulletinThemeColorSchemeTest {
    @Test
    fun defaultChoicesPreserveBothSemanticPalettesWithoutTinting() {
        assertEquals(LightColors, resolveBulletinColorScheme(darkTheme = false))
        assertEquals(DarkColors, resolveBulletinColorScheme(darkTheme = true))
    }

    @Test
    fun defaultAccentChangesWithAppearance() {
        val light = resolveBulletinColorScheme(darkTheme = false)
        val dark = resolveBulletinColorScheme(darkTheme = true)
        assertEquals(Color.hsl(208f, 0.31f, 0.35f), light.primary)
        assertEquals(Color.hsl(208f, 0.56f, 0.62f), dark.primary)
        assertEquals(Color.White, light.onPrimary)
        assertEquals(Color.hsl(210f, 0.36f, 0.10f), dark.onPrimary)
    }

    @Test
    fun customAccentPreservesSemanticSurfacesInBothModes() {
        val accent = BulletinThemeColor(BulletinPalette.FOREST.color.value)
        listOf(false, true).forEach { dark ->
            val defaults = resolveBulletinColorScheme(dark)
            val custom = resolveBulletinColorScheme(dark, themeColor = accent)
            assertEquals(accent.color, custom.primary)
            assertEquals(contentColorFor(accent.color), custom.onPrimary)
            assertNotEquals(defaults.primaryContainer, custom.primaryContainer)
            assertEquals(defaults.surface, custom.surface)
            assertEquals(defaults.background, custom.background)
            assertEquals(defaults.error, custom.error)
        }
    }

    @Test
    fun customBasePreservesModeSpecificAccentAndUpdatesSurfaceContrast() {
        val base = BulletinThemeColor(BulletinBasePalette.SAND.color.value)
        listOf(false, true).forEach { dark ->
            val defaults = resolveBulletinColorScheme(dark)
            val custom = resolveBulletinColorScheme(dark, baseColor = base)
            assertNotEquals(defaults.background, custom.background)
            assertNotEquals(defaults.surface, custom.surface)
            assertEquals(contentColorFor(custom.background), custom.onBackground)
            assertEquals(contentColorFor(custom.surface), custom.onSurface)
            assertEquals(defaults.primary, custom.primary)
            assertEquals(defaults.onPrimary, custom.onPrimary)
        }
    }
}
