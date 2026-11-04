package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ThemeHexFieldTest {
    @Test
    fun colorFormatsAsSixLowercaseRgbDigits() {
        assertEquals("4cb649", Color(0xFF4CB649).toHexRgbDigits())
    }

    @Test
    fun sixHexDigitsUpdateTheColorAndPreserveAlpha() {
        val updated = Color(0x804B3421).withHexRgb("4cb649")

        assertEquals(Color(0x804CB649), updated)
    }

    @Test
    fun incompleteOrInvalidHexDoesNotCreateAColor() {
        assertNull(Color.Black.withHexRgb("4cb64"))
        assertNull(Color.Black.withHexRgb("4cb64z"))
    }
}
