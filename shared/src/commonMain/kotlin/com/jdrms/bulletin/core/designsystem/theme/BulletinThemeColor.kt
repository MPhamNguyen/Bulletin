package com.jdrms.bulletin.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/** A user-selected accent color that can be stored as a stable Compose color value. */
@JvmInline
value class BulletinThemeColor(val value: ULong) {
    val color: Color get() = Color(value)

    companion object {
        val CLASSIC = BulletinThemeColor(BulletinPalette.CLASSIC.color.value)
        val BASE = BulletinThemeColor(BulletinBasePalette.CLOUD.color.value)
    }
}
