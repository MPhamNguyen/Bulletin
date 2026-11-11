package com.jdrms.bulletin.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Fixed artwork colors; independent of the application theme.
internal val PaletteMidBlue = Color(0xFF3E5C76)
internal val PaletteSlateBlue = Color(0xFF748CAB)

internal val LightColors = lightColorScheme(
    primary = Color.hsl(208f, 0.31f, 0.35f),
    onPrimary = Color.hsl(0f, 0.00f, 1.00f),
    background = Color.hsl(210f, 0.25f, 0.98f),
    onBackground = Color.hsl(210f, 0.34f, 0.12f),
    surface = Color.hsl(0f, 0.00f, 1.00f),
    onSurface = Color.hsl(210f, 0.34f, 0.12f),
    secondary = Color.hsl(208f, 0.22f, 0.92f),
    onSecondary = Color.hsl(208f, 0.35f, 0.25f),
    surfaceVariant = Color.hsl(210f, 0.18f, 0.94f),
    onSurfaceVariant = Color.hsl(210f, 0.14f, 0.43f),
    tertiary = Color.hsl(198f, 0.70f, 0.42f),
    onTertiary = Color.hsl(0f, 0.00f, 1.00f),
    outlineVariant = Color.hsl(210f, 0.18f, 0.88f),
    outline = Color.hsl(210f, 0.18f, 0.84f),
    error = Color.hsl(0f, 0.72f, 0.48f),
    onError = Color.hsl(0f, 0.00f, 1.00f),
    primaryContainer = Color.hsl(208f, 0.22f, 0.92f),
    onPrimaryContainer = Color.hsl(208f, 0.35f, 0.25f),
    secondaryContainer = Color.hsl(208f, 0.22f, 0.92f),
    onSecondaryContainer = Color.hsl(208f, 0.35f, 0.25f),
    tertiaryContainer = Color.hsl(208f, 0.22f, 0.92f),
    onTertiaryContainer = Color.hsl(208f, 0.35f, 0.25f),
    surfaceDim = Color.hsl(210f, 0.25f, 0.98f),
    surfaceBright = Color.hsl(0f, 0.00f, 1.00f),
    surfaceContainerLowest = Color.hsl(210f, 0.25f, 0.98f),
    surfaceContainerLow = Color.hsl(0f, 0.00f, 1.00f),
    surfaceContainer = Color.hsl(0f, 0.00f, 1.00f),
    surfaceContainerHigh = Color.hsl(210f, 0.18f, 0.94f),
    surfaceContainerHighest = Color.hsl(208f, 0.22f, 0.92f),
    surfaceTint = Color.hsl(208f, 0.31f, 0.35f),
    errorContainer = Color.hsl(210f, 0.18f, 0.94f),
    onErrorContainer = Color.hsl(0f, 0.72f, 0.48f),
    inverseSurface = Color.hsl(210f, 0.24f, 0.13f),
    inverseOnSurface = Color.hsl(210f, 0.20f, 0.96f),
    inversePrimary = Color.hsl(208f, 0.56f, 0.62f),
    scrim = Color.Black
)

internal val DarkColors = darkColorScheme(
    primary = Color.hsl(208f, 0.56f, 0.62f),
    onPrimary = Color.hsl(210f, 0.36f, 0.10f),
    background = Color.hsl(210f, 0.30f, 0.08f),
    onBackground = Color.hsl(210f, 0.20f, 0.96f),
    surface = Color.hsl(210f, 0.24f, 0.13f),
    onSurface = Color.hsl(210f, 0.20f, 0.96f),
    secondary = Color.hsl(208f, 0.22f, 0.20f),
    onSecondary = Color.hsl(208f, 0.28f, 0.92f),
    surfaceVariant = Color.hsl(210f, 0.18f, 0.18f),
    onSurfaceVariant = Color.hsl(210f, 0.14f, 0.66f),
    tertiary = Color.hsl(198f, 0.74f, 0.58f),
    onTertiary = Color.hsl(210f, 0.36f, 0.10f),
    outlineVariant = Color.hsl(210f, 0.18f, 0.22f),
    outline = Color.hsl(210f, 0.18f, 0.26f),
    error = Color.hsl(0f, 0.70f, 0.60f),
    onError = Color.hsl(0f, 0.00f, 1.00f),
    primaryContainer = Color.hsl(208f, 0.22f, 0.20f),
    onPrimaryContainer = Color.hsl(208f, 0.28f, 0.92f),
    secondaryContainer = Color.hsl(208f, 0.22f, 0.20f),
    onSecondaryContainer = Color.hsl(208f, 0.28f, 0.92f),
    tertiaryContainer = Color.hsl(208f, 0.22f, 0.20f),
    onTertiaryContainer = Color.hsl(208f, 0.28f, 0.92f),
    surfaceDim = Color.hsl(210f, 0.30f, 0.08f),
    surfaceBright = Color.hsl(210f, 0.24f, 0.13f),
    surfaceContainerLowest = Color.hsl(210f, 0.30f, 0.08f),
    surfaceContainerLow = Color.hsl(210f, 0.24f, 0.13f),
    surfaceContainer = Color.hsl(210f, 0.24f, 0.13f),
    surfaceContainerHigh = Color.hsl(210f, 0.18f, 0.18f),
    surfaceContainerHighest = Color.hsl(208f, 0.22f, 0.20f),
    surfaceTint = Color.hsl(208f, 0.56f, 0.62f),
    errorContainer = Color.hsl(210f, 0.18f, 0.18f),
    onErrorContainer = Color.hsl(0f, 0.70f, 0.60f),
    inverseSurface = Color.hsl(0f, 0.00f, 1.00f),
    inverseOnSurface = Color.hsl(210f, 0.34f, 0.12f),
    inversePrimary = Color.hsl(208f, 0.31f, 0.35f),
    scrim = Color.Black
)
