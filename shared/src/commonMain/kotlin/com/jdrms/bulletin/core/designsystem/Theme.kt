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
//   0D1321 — near-black navy, primary text / dark-mode surface
//   748CAB — muted slate-blue, secondary accents, borders, containers
// ---------------------------------------------------------------------------
private val PaletteOffWhite = Color(0xFFF5F5F5)
private val PaletteMidBlue = Color(0xFF3E5C76)
private val PaletteInk = Color(0xFF0D1321)
private val PaletteSlateBlue = Color(0xFF748CAB)

// Derived tints/shades (not in the original 4, but needed to fill out a full
// M3 scheme — each is a lightened/darkened step off one of the four core hues)
private val MidBlueDark = Color(0xFF2C4457) // pressed/dark-mode primary
private val MidBlueContainer = Color(0xFFDCE4EB) // light tint of 3E5C76
private val SlateBlueContainer = Color(0xFFE7ECF2) // light tint of 748CAB
private val SlateBlueDark = Color(0xFF56698A) // deepened 748CAB for dark mode

private val CardBackground = Color(0xFFFFFFFF)
private val BorderSubtle = Color(0xFFDDE2E8)

// Gold and green aren't part of the 4-color palette but are still needed for
// star ratings and success states (e.g. "Profile Updated") — kept muted/close
// in value to 748CAB so they read as accents rather than a clashing 5th color.
private val AccentGold = Color(0xFFFFD700)
private val AccentGoldContainer = Color(0xFFF3E4BF)
private val AccentSuccess = Color(0xFF4ADE80)
private val ModalDarkBackground = Color(0xFF1E293B)
private val AccentError = Color(0xFFB3261E)

private val LightColors = lightColorScheme(
    primary = PaletteMidBlue,
    onPrimary = PaletteOffWhite,
    primaryContainer = MidBlueContainer,
    onPrimaryContainer = PaletteInk,

    secondary = PaletteSlateBlue,
    onSecondary = Color.White,
    secondaryContainer = SlateBlueContainer,
    onSecondaryContainer = PaletteInk,

    tertiary = AccentGold,
    onTertiary = Color(0xFF3D2F00),
    tertiaryContainer = AccentGoldContainer,
    onTertiaryContainer = Color(0xFF3D2F00),

    background = PaletteOffWhite,
    onBackground = PaletteInk,

    surface = CardBackground,
    onSurface = PaletteInk,
    surfaceVariant = Color(0xFFEFF1F4),
    onSurfaceVariant = Color(0xFF4C5A6B),

    outline = BorderSubtle,
    outlineVariant = Color(0xFFEAEDF1),

    error = AccentError,
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),

    inverseSurface = PaletteInk,
    inverseOnSurface = PaletteOffWhite,
    inversePrimary = PaletteSlateBlue,

    scrim = Color.Black
)

private val DarkColors = darkColorScheme(
    primary = PaletteSlateBlue,
    onPrimary = PaletteInk,
    primaryContainer = MidBlueDark,
    onPrimaryContainer = SlateBlueContainer,

    secondary = SlateBlueDark,
    onSecondary = PaletteOffWhite,
    secondaryContainer = Color(0xFF2C3A4E),
    onSecondaryContainer = SlateBlueContainer,

    tertiary = Color(0xFFE0B85C),
    onTertiary = Color(0xFF402D00),
    tertiaryContainer = Color(0xFF5C4300),
    onTertiaryContainer = AccentGoldContainer,

    background = PaletteInk,
    onBackground = PaletteOffWhite,

    surface = Color(0xFF16202E),
    onSurface = PaletteOffWhite,
    surfaceVariant = Color(0xFF2C3A4E),
    onSurfaceVariant = Color(0xFFBCC6D4),

    outline = Color(0xFF5C6B80),
    outlineVariant = Color(0xFF2C3A4E),

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
// Semantic "success" color — M3's ColorScheme has no success slot, so it's
// provided as a small side-channel. Used for confirmation states like the
// "Profile Updated" pill.
// ---------------------------------------------------------------------------
data class BulletinExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color
)

private val LightExtendedColors = BulletinExtendedColors(
    success = AccentSuccess,
    onSuccess = Color.White,
    successContainer = ModalDarkBackground,
    onSuccessContainer = PaletteOffWhite
)

private val DarkExtendedColors = BulletinExtendedColors(
    success = Color(0xFF8FCBAE),
    onSuccess = Color(0xFF0F3D28),
    successContainer = ModalDarkBackground,
    onSuccessContainer = PaletteOffWhite
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
    // Create Account, Update) use `primary` (3E5C76), not tertiary/gold.
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
