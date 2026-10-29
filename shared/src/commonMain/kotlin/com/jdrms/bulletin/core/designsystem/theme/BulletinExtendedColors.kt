package com.jdrms.bulletin.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Gold is reserved for star ratings ONLY. Never use it for containers,
// buttons, avatars or highlights.
private val StarLight = Color(0xFFF5B301)
private val StarDark = Color(0xFFFBBF24)

// ---------------------------------------------------------------------------
// Extended colors — M3's ColorScheme has no success or star slot, so they are
// provided as a small side-channel.
//   success*: confirmation states ("Profile Updated", Verified badge)
//             dark values = design's tertiary-fixed-dim / -fixed tokens
//   star:     the ONLY place gold is used in the app (rating stars)
// ---------------------------------------------------------------------------
data class BulletinExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val star: Color
)

internal val LightExtendedColors = BulletinExtendedColors(
    success = Color(0xFF1E7B4C),
    onSuccess = Color.White,
    successContainer = Color(0xFFDDF3E6),
    onSuccessContainer = Color(0xFF0B3D24),
    star = StarLight
)

internal val DarkExtendedColors = BulletinExtendedColors(
    success = Color(0xFF34D399),
    onSuccess = Color(0xFF0A2B1D),
    successContainer = Color(0xFF123629),
    onSuccessContainer = Color(0xFF6EE7B7),
    star = StarDark
)

val LocalBulletinExtendedColors = staticCompositionLocalOf { LightExtendedColors }

object BulletinExtras {
    val colors: BulletinExtendedColors
        @Composable get() = LocalBulletinExtendedColors.current
}
