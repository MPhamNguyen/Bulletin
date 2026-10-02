package com.jdrms.bulletin.core.designsystem

import androidx.compose.runtime.Composable

data class SystemBarAppearance(
    val isAppearanceLightStatusBars: Boolean,
    val isAppearanceLightNavigationBars: Boolean
)

fun resolveSystemBarAppearance(isDarkTheme: Boolean): SystemBarAppearance = SystemBarAppearance(
    isAppearanceLightStatusBars = !isDarkTheme,
    isAppearanceLightNavigationBars = !isDarkTheme
)

@Composable
expect fun PlatformSystemBarAppearance(isDarkTheme: Boolean)
