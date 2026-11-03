package com.jdrms.bulletin.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.jdrms.bulletin.core.designsystem.PlatformSystemBarAppearance

/** Provides the app color scheme, typography, shapes, and extended colors. */
@Composable
fun BulletinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeColor: BulletinThemeColor = BulletinThemeColor.CLASSIC,
    content: @Composable () -> Unit
) {
    PlatformSystemBarAppearance(isDarkTheme = darkTheme)
    val baseColorScheme = if (darkTheme) DarkColors else LightColors
    val colorScheme = baseColorScheme.copy(
        primary = themeColor.color,
        onPrimary = if (themeColor.color.luminance() > 0.45f) Color.Black else Color.White
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
