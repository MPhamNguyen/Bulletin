package com.jdrms.bulletin.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Shapes
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// Core brand palette (as given):
//   F5F5F5 — off-white, app background / light surfaces
//   3E5C76 — mid navy-blue, primary actions (buttons, active states)
//   0D1321 — near-black navy, primary text / dark-mode background
//   748CAB — muted slate-blue, secondary accents, borders, containers
// ---------------------------------------------------------------------------
private val PaletteOffWhite = Color(0xFFF5F5F5)
private val PaletteMidBlue = Color(0xFF3E5C76)
private val PaletteInk = Color(0xFF0D1321)
private val PaletteSlateBlue = Color(0xFF748CAB)

// Derived tints/shades (light)
private val MidBlueContainer = Color(0xFFDCE4EB)
private val SlateBlueContainer = Color(0xFFE7ECF2)
private val CardBackground = Color(0xFFFFFFFF)
private val BorderSubtle = Color(0xFFDDE2E8)

// ---------------------------------------------------------------------------
// Dark theme — calibrated collegiate dark mode with clear tactile depth between
// the canvas background, card surfaces and interactive controls.
// ---------------------------------------------------------------------------
private val DarkCanvasBackground = Color(0xFF0B0F19)
private val DarkSurfaceLowest = Color(0xFF070B13)
private val DarkSurfaceLow = Color(0xFF111726)
private val DarkSurface = Color(0xFF161F33)
private val DarkSurfaceContainer = Color(0xFF161F33)
private val DarkSurfaceHigh = Color(0xFF1B263E)
private val DarkSurfaceHighest = Color(0xFF1D2A45)
private val DarkSurfaceVariant = Color(0xFF1E293B)
private val DarkSurfaceBright = Color(0xFF263758)

// Gold is reserved for star ratings ONLY. Never use it for containers,
// buttons, avatars or highlights.
private val StarLight = Color(0xFFF5B301)
private val StarDark = Color(0xFFFBBF24)

private val AccentError = Color(0xFFB3261E)

private val LightColors = lightColorScheme(
    primary = PaletteMidBlue,
    onPrimary = PaletteOffWhite,
    primaryContainer = MidBlueContainer,
    onPrimaryContainer = PaletteInk,

    secondary = PaletteSlateBlue,
    onSecondary = PaletteInk,
    secondaryContainer = SlateBlueContainer,
    onSecondaryContainer = PaletteInk,

    tertiary = Color(0xFF3F7F86),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD9ECEE),
    onTertiaryContainer = Color(0xFF0F2F33),

    background = PaletteOffWhite,
    onBackground = PaletteInk,

    surface = CardBackground,
    onSurface = PaletteInk,
    surfaceVariant = Color(0xFFEFF1F4),
    onSurfaceVariant = Color(0xFF4C5A6B),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFAFB),
    surfaceContainer = Color(0xFFF5F6F8),
    surfaceContainerHigh = Color(0xFFEFF1F4),
    surfaceContainerHighest = Color(0xFFE7ECF2),

    // outline = controls (switch track, input borders, chip outlines)
    // outlineVariant = card borders and dividers
    outline = Color(0xFF8392A6),
    outlineVariant = BorderSubtle,

    error = AccentError,
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),

    inverseSurface = PaletteInk,
    inverseOnSurface = PaletteOffWhite,
    inversePrimary = Color(0xFF9DB6D6),

    scrim = Color.Black
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5B8DEF),
    onPrimary = DarkCanvasBackground,
    primaryContainer = Color(0xFF17243D),
    onPrimaryContainer = Color(0xFFAFC8FF),

    secondary = Color(0xFF8FA8C8),
    onSecondary = Color(0xFF0F1A28),
    secondaryContainer = Color(0xFF243348),
    onSecondaryContainer = Color(0xFFD3E0F0),

    tertiary = Color(0xFF38BDF8),
    onTertiary = DarkCanvasBackground,
    tertiaryContainer = Color(0xFF102B3A),
    onTertiaryContainer = Color(0xFFBAE6FD),

    background = DarkCanvasBackground,
    onBackground = Color(0xFFF0F4F8),

    surface = DarkSurface,
    onSurface = Color(0xFFF0F4F8),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF8F9FB5),
    surfaceDim = DarkCanvasBackground,
    surfaceBright = DarkSurfaceBright,
    surfaceContainerLowest = DarkSurfaceLowest,
    surfaceContainerLow = DarkSurfaceLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceHigh,
    surfaceContainerHighest = DarkSurfaceHighest,

    outline = Color(0xFF5E7292),
    outlineVariant = Color(0xFF1E293B),

    error = Color(0xFFFB7185),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    inverseSurface = Color(0xFFF0F4F8),
    inverseOnSurface = Color(0xFF121824),
    inversePrimary = PaletteMidBlue,

    surfaceTint = Color(0xFF5B8DEF),

    scrim = Color.Black
)

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

private val LightExtendedColors = BulletinExtendedColors(
    success = Color(0xFF1E7B4C),
    onSuccess = Color.White,
    successContainer = Color(0xFFDDF3E6),
    onSuccessContainer = Color(0xFF0B3D24),
    star = StarLight
)

private val DarkExtendedColors = BulletinExtendedColors(
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
private val BulletinTypography = Typography(
    displaySmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

private val BulletinShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = CircleShape
)

object BulletinTextFieldDefaults {
    @Composable
    fun colors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
    )
}

object BulletinInactiveButtonDefaults {
    @Composable
    fun colors(): ButtonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        contentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.40f),
        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)
    )
}

object BulletinButtonDefaults {
    // Primary CTAs (Sign in, Verify Email, Post Listing, Message Seller,
    // Create Account, Update) use `primary`, not tertiary.
    @Composable
    fun buttonColors(): ButtonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    )

    @Composable
    fun inactiveButtonColors(): ButtonColors = BulletinInactiveButtonDefaults.colors()

    @Composable
    fun outlinedButtonColors(): ButtonColors = ButtonDefaults.outlinedButtonColors(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary
    )

    @Composable
    fun outlinedButtonBorder(): BorderStroke = BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )

    @Composable
    fun destructiveOutlinedButtonColors(): ButtonColors = ButtonDefaults.outlinedButtonColors(
        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
        contentColor = MaterialTheme.colorScheme.error
    )

    @Composable
    fun destructiveOutlinedButtonBorder(): BorderStroke = BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
    )

    @Composable
    fun textButtonColors(): ButtonColors = ButtonDefaults.textButtonColors(
        contentColor = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun BulletinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    PlatformSystemBarAppearance(isDarkTheme = darkTheme)
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(
        LocalBulletinExtendedColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = BulletinTypography,
            shapes = BulletinShapes,
            content = content
        )
    }
}
