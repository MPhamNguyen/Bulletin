package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThemeColorWheelColorTest {
    @Test
    fun centerSelectionIsWhiteAndHasZeroSaturation() {
        val centerColor = themeWheelColorAt(hue = 120f, saturation = 0f)

        assertEquals(Color.White, centerColor)
        assertEquals(0f, themeWheelHueAndSaturation(centerColor).second)
    }

    @Test
    fun innerSelectionKeepsItsSaturationWhenMarkerPositionIsRestored() {
        val selectedColor = themeWheelColorAt(hue = 0f, saturation = 0.5f)
        val (hue, saturation) = themeWheelHueAndSaturation(selectedColor)

        assertEquals(0f, hue)
        assertTrue(
            kotlin.math.abs(saturation - 0.5f) < 0.003f,
            "Expected half saturation, got $saturation from $selectedColor"
        )
    }

    @Test
    fun darkerColorsKeepTheirBrightnessWhenMappedBackToTheWheel() {
        val selectedColor = Color(0.3f, 0.15f, 0.15f)
        val (hue, saturation, brightness) = themeWheelHueSaturationAndBrightness(selectedColor)

        val restoredColor = themeWheelColorAt(hue, saturation, brightness)

        assertTrue(kotlin.math.abs(selectedColor.red - restoredColor.red) < 0.001f)
        assertTrue(kotlin.math.abs(selectedColor.green - restoredColor.green) < 0.001f)
        assertTrue(kotlin.math.abs(selectedColor.blue - restoredColor.blue) < 0.001f)
    }
}
