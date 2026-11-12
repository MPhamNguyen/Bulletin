package com.jdrms.bulletin.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
    val colorScheme = resolveBulletinColorScheme(darkTheme, themeColor, baseColor)
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
