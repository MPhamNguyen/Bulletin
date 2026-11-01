package com.jdrms.bulletin.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
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
    val star: Color,
    val unreadGradientStart: Color,
    val unreadGradientEnd: Color,
    val unreadBorder: Color
)

internal val LightExtendedColors = BulletinExtendedColors(
    success = Color(0xFF1E7B4C),
    onSuccess = Color.White,
    successContainer = Color(0xFFDDF3E6),
    onSuccessContainer = Color(0xFF0B3D24),
    star = StarLight,
    unreadGradientStart = Color(0xFFDCEBFF),
    unreadGradientEnd = Color(0xFFDDF3F0),
    unreadBorder = Color(0xFF3E5C76)
)

internal val DarkExtendedColors = BulletinExtendedColors(
    success = Color(0xFF34D399),
    onSuccess = Color(0xFF0A2B1D),
    successContainer = Color(0xFF123629),
    onSuccessContainer = Color(0xFF6EE7B7),
    star = StarDark,
    unreadGradientStart = Color(0xFF193A67),
    unreadGradientEnd = Color(0xFF22495D),
    unreadBorder = Color(0xFF77A7F7)
)

internal val LocalBulletinExtendedColors = staticCompositionLocalOf { LightExtendedColors }

/** Access colors not provided by Material's ColorScheme. */
val MaterialTheme.bulletinColors: BulletinExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalBulletinExtendedColors.current

/** Fixed third-party brand colors. */
object BulletinBrandColors {
    val microsoftRed = Color(0xFFF25022)
    val microsoftGreen = Color(0xFF7FBA00)
    val microsoftBlue = Color(0xFF00A4EF)
    val microsoftYellow = Color(0xFFFFB900)
}
