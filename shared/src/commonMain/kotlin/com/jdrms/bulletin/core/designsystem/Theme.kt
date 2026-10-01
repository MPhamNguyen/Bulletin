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

// Derived tints/shades (dark) — a stepped surface ladder so cards, listings,
// the nav bar and modals each read as a distinct layer over the page.
private val DarkSurface = Color(0xFF182234) // cards
private val DarkSurfaceLowest = Color(0xFF0A0F1B)
private val DarkSurfaceLow = Color(0xFF131B2B) // bottom nav
private val DarkSurfaceHigh = Color(0xFF1F2B40) // modals, menus
private val DarkSurfaceHighest = Color(0xFF27354D)
private val DarkSurfaceVariant = Color(0xFF263349) // inputs, chips, Edit button

// Gold is reserved for star ratings ONLY. Never use it for containers,
// buttons, avatars or highlights.
private val StarLight = Color(0xFFF5B301)
private val StarDark = Color(0xFFFFD54A)

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
    primary = Color(0xFF9DB6D6),
    onPrimary = PaletteInk,
    primaryContainer = Color(0xFF2F4660),
    onPrimaryContainer = Color(0xFFE8EEF5),

    secondary = Color(0xFF8CA0BD),
    onSecondary = PaletteInk,
    secondaryContainer = Color(0xFF243246),
    onSecondaryContainer = Color(0xFFE7ECF2),

    tertiary = Color(0xFF7FC4C9),
    onTertiary = Color(0xFF0A2A2D),
    tertiaryContainer = Color(0xFF1F4A4F),
    onTertiaryContainer = Color(0xFFD2EEF0),

    background = PaletteInk,
    onBackground = PaletteOffWhite,

    surface = DarkSurface,
    onSurface = PaletteOffWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFA9B8D0),
    surfaceContainerLowest = DarkSurfaceLowest,
    surfaceContainerLow = DarkSurfaceLow,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurfaceHigh,
    surfaceContainerHighest = DarkSurfaceHighest,

    outline = Color(0xFF6B7F9C),
    outlineVariant = Color(0xFF34445E),

    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),

    inverseSurface = PaletteOffWhite,
    inverseOnSurface = PaletteInk,
    inversePrimary = PaletteMidBlue,

    scrim = Color.Black
)

// ---------------------------------------------------------------------------
// Extended colors — M3's ColorScheme has no success or star slot, so they are
// provided as a small side-channel.
//   success*: confirmation states ("Profile Updated", Verified badge)
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
    success = Color(0xFF8FCBAE),
    onSuccess = Color(0xFF0F3D28),
    successContainer = Color(0xFF1C3A2E),
    onSuccessContainer = Color(0xFFD5F0E2),
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
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = CircleShape
)

object BulletinTextFieldDefaults {
    @Composable
    fun colors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
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
        width = 1.5.dp,
        color = MaterialTheme.colorScheme.primary
    )

    @Composable
    fun destructiveOutlinedButtonColors(): ButtonColors = ButtonDefaults.outlinedButtonColors(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.error
    )

    @Composable
    fun destructiveOutlinedButtonBorder(): BorderStroke = BorderStroke(
        width = 1.5.dp,
        color = MaterialTheme.colorScheme.error
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
