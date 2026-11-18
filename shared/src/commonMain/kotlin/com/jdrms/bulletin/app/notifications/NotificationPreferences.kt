package com.jdrms.bulletin.app.notifications

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Device-wide preference for system alerts; foreground banners are independent. */
class NotificationPreferences(
    initiallyEnabled: Boolean = true,
    private val persist: (Boolean) -> Unit = {},
    private val clearNotifications: () -> Unit = {}
) {
    private val mutableEnabled = MutableStateFlow(initiallyEnabled)
    val enabled = mutableEnabled.asStateFlow()

    fun setEnabled(enabled: Boolean) {
        persist(enabled)
        mutableEnabled.update { enabled }
        if (!enabled) clearNotifications()
    }
}

val LocalNotificationPreferences = staticCompositionLocalOf<NotificationPreferences> {
    NotificationPreferences()
}
