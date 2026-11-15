package com.jdrms.bulletin.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance

/** Default choices retain the exact semantic palette; custom choices tint only their own roles. */
internal fun resolveBulletinColorScheme(
    darkTheme: Boolean,
    themeColor: BulletinThemeColor = BulletinThemeColor.CLASSIC,
    baseColor: BulletinThemeColor = BulletinThemeColor.BASE
): ColorScheme {
    val baseColorScheme = if (darkTheme) DarkColors else LightColors
    val scheme = if (baseColor == BulletinThemeColor.BASE) {
        baseColorScheme
    } else {
        tintBaseSurfaces(baseColorScheme, baseColor.color)
    }
    if (themeColor == BulletinThemeColor.CLASSIC) return scheme
    val primaryContainer = derivePrimaryContainerColor(themeColor.color, scheme.surface, darkTheme)
    return scheme.copy(
        primary = themeColor.color,
        onPrimary = contentColorFor(themeColor.color),
        primaryContainer = primaryContainer,
        onPrimaryContainer = contentColorFor(primaryContainer)
    )
}

private fun tintBaseSurfaces(scheme: ColorScheme, base: Color): ColorScheme {
    val background = base.copy(alpha = 0.12f).compositeOver(scheme.background)
    val surface = base.copy(alpha = 0.06f).compositeOver(scheme.surface)
    return scheme.copy(
        background = background,
        onBackground = contentColorFor(background),
        surface = surface,
        onSurface = contentColorFor(surface),
        surfaceVariant = base.copy(alpha = 0.08f).compositeOver(scheme.surfaceVariant),
        surfaceContainerLow = base.copy(alpha = 0.06f).compositeOver(scheme.surfaceContainerLow),
        surfaceContainer = base.copy(alpha = 0.08f).compositeOver(scheme.surfaceContainer),
        surfaceContainerHigh = base.copy(alpha = 0.10f).compositeOver(scheme.surfaceContainerHigh),
        surfaceContainerHighest = base.copy(alpha = 0.12f).compositeOver(scheme.surfaceContainerHighest)
    )
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
