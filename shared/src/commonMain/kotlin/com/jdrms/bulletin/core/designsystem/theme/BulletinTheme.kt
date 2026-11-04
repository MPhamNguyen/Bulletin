package com.jdrms.bulletin.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.jdrms.bulletin.core.designsystem.PlatformSystemBarAppearance

/** Provides the app color scheme, typography, shapes, and extended colors. */
@Composable
fun BulletinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeColor: BulletinThemeColor = BulletinThemeColor.CLASSIC,
    baseColor: BulletinThemeColor = BulletinThemeColor.BASE,
    content: @Composable () -> Unit
) {
    PlatformSystemBarAppearance(isDarkTheme = darkTheme)
    val baseColorScheme = if (darkTheme) DarkColors else LightColors
    val tintedBackground = baseColor.color.copy(alpha = 0.12f).compositeOver(baseColorScheme.background)
    val colorScheme = baseColorScheme.copy(
        primary = themeColor.color,
        onPrimary = if (themeColor.color.luminance() > 0.45f) Color.Black else Color.White,
        background = tintedBackground,
        onBackground = if (tintedBackground.luminance() > 0.45f) Color.Black else Color.White,
        surface = baseColor.color.copy(alpha = 0.06f).compositeOver(baseColorScheme.surface),
        surfaceVariant = baseColor.color.copy(alpha = 0.08f).compositeOver(baseColorScheme.surfaceVariant),
        surfaceContainerLow = baseColor.color.copy(alpha = 0.06f)
            .compositeOver(baseColorScheme.surfaceContainerLow),
        surfaceContainer = baseColor.color.copy(alpha = 0.08f)
            .compositeOver(baseColorScheme.surfaceContainer),
        surfaceContainerHigh = baseColor.color.copy(alpha = 0.10f)
            .compositeOver(baseColorScheme.surfaceContainerHigh),
        surfaceContainerHighest = baseColor.color.copy(alpha = 0.12f)
            .compositeOver(baseColorScheme.surfaceContainerHighest)
    )
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(
        LocalBulletinExtendedColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = BulletinTypography,
            shapes = BulletinMaterialShapes,
            content = content
        )
    }
}
