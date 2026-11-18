package com.jdrms.bulletin.app.notifications

import androidx.compose.runtime.staticCompositionLocalOf

/** Platform notification permission, independent of the saved device preference. */
data class NotificationPermissionStatus(
    val allowed: Boolean = true,
    val openSettings: () -> Unit = {}
)

val LocalNotificationPermissionStatus = staticCompositionLocalOf { NotificationPermissionStatus() }
