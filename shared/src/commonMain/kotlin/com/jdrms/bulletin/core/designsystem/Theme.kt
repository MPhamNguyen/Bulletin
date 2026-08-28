package com.jdrms.bulletin.core.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Brand colors
private val BulletinBlue = Color(0xFF365F78)
private val BulletinBlueDark = Color(0xFF23465B)
private val BulletinBlueLight = Color(0xFFD3E7F3)

private val BulletinTeal = Color(0xFF0F766E)
private val BulletinTealLight = Color(0xFFCCFBF1)

private val BulletinGold = Color(0xFFF5B700)
private val BulletinRed = Color(0xFFBA1A1A)

// Neutral colors
private val Ink = Color(0xFF17212B)
private val Slate = Color(0xFF52606D)
private val Border = Color(0xFFB8C5CE)
private val AppBackground = Color(0xFFF7F9FB)
private val CardBackground = Color(0xFFFFFFFF)

private val LightColors = lightColorScheme(
    primary = BulletinBlue,
    onPrimary = Color.White,
    primaryContainer = BulletinBlueLight,
    onPrimaryContainer = BulletinBlueDark,

    secondary = BulletinTeal,
    onSecondary = Color.White,
    secondaryContainer = BulletinTealLight,
    onSecondaryContainer = Color(0xFF00201D),

    tertiary = BulletinGold,
    onTertiary = Color(0xFF3D2F00),
    tertiaryContainer = Color(0xFFFFE08A),
    onTertiaryContainer = Color(0xFF251A00),

    background = AppBackground,
    onBackground = Ink,

    surface = CardBackground,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE8EEF2),
    onSurfaceVariant = Slate,

    outline = Border,
    outlineVariant = Color(0xFFD8E0E5),

    error = BulletinRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    inverseSurface = Color(0xFF26323A),
    inverseOnSurface = Color(0xFFEDF1F4),
    inversePrimary = Color(0xFFA2CDE5),

    scrim = Color.Black
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA2CDE5),
    onPrimary = Color(0xFF003548),
    primaryContainer = BulletinBlueDark,
    onPrimaryContainer = Color(0xFFD3E7F3),

    secondary = Color(0xFF7ED7CC),
    onSecondary = Color(0xFF003733),
    secondaryContainer = Color(0xFF00504A),
    onSecondaryContainer = BulletinTealLight,

    tertiary = Color(0xFFFFD15C),
    onTertiary = Color(0xFF402D00),
    tertiaryContainer = Color(0xFF5C4300),
    onTertiaryContainer = Color(0xFFFFE08A),

    background = Color(0xFF101417),
    onBackground = Color(0xFFE1E7EB),

    surface = Color(0xFF171C20),
    onSurface = Color(0xFFE1E7EB),
    surfaceVariant = Color(0xFF3E484E),
    onSurfaceVariant = Color(0xFFBEC8CE),

    outline = Color(0xFF899299),
    outlineVariant = Color(0xFF3E484E),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

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
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun BulletinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current

            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = BulletinTypography,
        shapes = BulletinShapes,
        content = content
    )
}
