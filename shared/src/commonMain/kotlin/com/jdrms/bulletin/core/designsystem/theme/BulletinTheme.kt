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
    val tintedSurface = baseColor.color.copy(alpha = 0.06f).compositeOver(baseColorScheme.surface)
    val primaryContainer = derivePrimaryContainerColor(themeColor.color, tintedSurface, darkTheme)
    val colorScheme = baseColorScheme.copy(
        primary = themeColor.color,
        onPrimary = contentColorFor(themeColor.color),
        primaryContainer = primaryContainer,
        onPrimaryContainer = contentColorFor(primaryContainer),
        background = tintedBackground,
        onBackground = contentColorFor(tintedBackground),
        surface = tintedSurface,
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

internal fun contentColorFor(background: Color): Color =
    if (background.luminance() > MINIMUM_LIGHTNESS_FOR_BLACK_TEXT) Color.Black else Color.White

internal fun derivePrimaryContainerColor(primary: Color, surface: Color, darkTheme: Boolean): Color {
    val tintAmount = if (darkTheme) DARK_PRIMARY_CONTAINER_TINT else LIGHT_PRIMARY_CONTAINER_TINT
    return primary.copy(alpha = tintAmount).compositeOver(surface)
}

private const val MINIMUM_LIGHTNESS_FOR_BLACK_TEXT = 0.179f
private const val LIGHT_PRIMARY_CONTAINER_TINT = 0.16f
private const val DARK_PRIMARY_CONTAINER_TINT = 0.32f
