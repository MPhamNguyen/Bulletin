package com.jdrms.bulletin.domain.profile.presentation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jdrms.bulletin.app.notifications.LocalNotificationPermissionStatus
import com.jdrms.bulletin.app.notifications.NotificationPermissionStatus
import com.jdrms.bulletin.core.designsystem.theme.BulletinTheme
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class NotificationSettingsDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun blockedPermissionShowsSettingsActionAndClearsWhenAllowed() {
        val allowed = mutableStateOf(false)
        var settingsOpened = 0
        compose.setContent {
            CompositionLocalProvider(
                LocalNotificationPermissionStatus provides NotificationPermissionStatus(
                    allowed = allowed.value,
                    openSettings = { settingsOpened++ }
                )
            ) {
                BulletinTheme { NotificationsView(onBack = {}) }
            }
        }
        compose.onNodeWithText("Push notifications").assertIsDisplayed()
        compose.onNodeWithText("Messages and matching listings").assertIsDisplayed()
        compose.onNodeWithText("Notifications are blocked in Android settings").assertIsDisplayed()
        compose.onNodeWithText("Allow in settings").performClick()
        compose.runOnIdle {
            assertEquals(1, settingsOpened)
            allowed.value = true
        }
        compose.onNodeWithText("Allow in settings").assertDoesNotExist()
        compose.onNodeWithText("Notifications are blocked in Android settings").assertDoesNotExist()
    }
}
