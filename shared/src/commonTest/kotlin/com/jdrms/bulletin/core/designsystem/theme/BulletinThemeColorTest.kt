package com.jdrms.bulletin.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class BulletinThemeColorTest {
    @Test
    fun baseColorUsesAValidComposeColorValue() {
        assertEquals(Color(0xFFF5F5F5), BulletinThemeColor.BASE.color)
    }
}
