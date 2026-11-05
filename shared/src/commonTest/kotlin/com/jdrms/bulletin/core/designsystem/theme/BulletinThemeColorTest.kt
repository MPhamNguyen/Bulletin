package com.jdrms.bulletin.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class BulletinThemeColorTest {
    @Test
    fun baseColorUsesAValidComposeColorValue() {
        assertEquals(Color(0xFFF5F5F5), BulletinThemeColor.BASE.color)
    }

    @Test
    fun contentColorMaintainsContrastForMidToneBackgrounds() {
        assertEquals(Color.Black, contentColorFor(Color(0xFF808080)))
        assertEquals(Color.White, contentColorFor(Color(0xFF757575)))
    }

    @Test
    fun primaryContainerUsesTheSelectedAccentInBothThemes() {
        val accent = Color(0xFF356859)
        val lightContainer = derivePrimaryContainerColor(accent, LightColors.surface, darkTheme = false)
        val darkContainer = derivePrimaryContainerColor(accent, DarkColors.surface, darkTheme = true)

        assertNotEquals(LightColors.primaryContainer, lightContainer)
        assertNotEquals(DarkColors.primaryContainer, darkContainer)
        assertEquals(Color.Black, contentColorFor(lightContainer))
        assertEquals(Color.White, contentColorFor(darkContainer))
    }
}
