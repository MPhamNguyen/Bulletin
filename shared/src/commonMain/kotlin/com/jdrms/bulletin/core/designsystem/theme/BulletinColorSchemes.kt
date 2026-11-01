package com.jdrms.bulletin.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Core brand palette (as given):
//   F5F5F5 — off-white, app background / light surfaces
//   3E5C76 — mid navy-blue, primary actions (buttons, active states)
//   0D1321 — near-black navy, primary text / dark-mode background
//   748CAB — muted slate-blue, secondary accents, borders, containers
private val PaletteOffWhite = Color(0xFFF5F5F5)
internal val PaletteMidBlue = Color(0xFF3E5C76)
private val PaletteInk = Color(0xFF0D1321)
internal val PaletteSlateBlue = Color(0xFF748CAB)

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

private val AccentError = Color(0xFFB3261E)

internal val LightColors = lightColorScheme(
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

internal val DarkColors = darkColorScheme(
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
