package com.jdrms.bulletin.core.designsystem

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
actual fun PlatformSystemBarAppearance(isDarkTheme: Boolean) {
    if (LocalInspectionMode.current) return

    val view = LocalView.current
    if (view.isInEditMode) return

    val context = LocalContext.current
    DisposableEffect(isDarkTheme, view, context) {
        val activity = context.findActivity()
        applySystemBarAppearance(activity = activity, view = view, isDarkTheme = isDarkTheme)
        onDispose {}
    }
}

internal fun applySystemBarAppearance(
    activity: Activity?,
    view: View,
    isDarkTheme: Boolean
) {
    if (activity == null || activity.isFinishing) return

    if (activity is ComponentActivity) {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
                detectDarkMode = { isDarkTheme }
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
                detectDarkMode = { isDarkTheme }
            )
        )
    }

    val window = activity.window ?: return
    val insetsController = WindowCompat.getInsetsController(window, view)
    val appearance = resolveSystemBarAppearance(isDarkTheme)
    insetsController.isAppearanceLightStatusBars = appearance.isAppearanceLightStatusBars
    insetsController.isAppearanceLightNavigationBars = appearance.isAppearanceLightNavigationBars
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext?.findActivity()
    else -> null
}
