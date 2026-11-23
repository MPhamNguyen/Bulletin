package com.jdrms.bulletin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.jdrms.bulletin.app.App
import com.jdrms.bulletin.app.notifications.LocalNotificationPreferences
import com.jdrms.bulletin.notifications.MarketplaceInterestNotificationIntents
import com.jdrms.bulletin.notifications.MessageNotificationIntents
import com.jdrms.bulletin.notifications.NotificationSettingsHost
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val SPLASH_MAX_WAIT_MILLIS = 3_000L

class MainActivity : ComponentActivity() {
    private val bulletin get() = application as BulletinApplication
    private var leftForBrowser = false
    private var splashDone = false
    private val notificationPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* Alerts simply stay off if denied. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate. Holds the splash until the stored session is resolved so Home or sign-in
        // appears directly, rather than a spinner. Capped so a slow network cannot keep users on the splash.
        installSplashScreen().setKeepOnScreenCondition { !splashDone }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            withTimeoutOrNull(SPLASH_MAX_WAIT_MILLIS) { bulletin.container.isSessionResolved.first { it } }
            splashDone = true
        }
        if (savedInstanceState == null) handleNotificationTap(intent)
        handleCallback(intent)
        bulletin.container.startMessageNotifications(bulletin.messageNotifier)
        bulletin.container.startMarketplaceInterestNotifications()
        requestNotificationPermissionAfterSignIn()
        setContent {
            CompositionLocalProvider(LocalNotificationPreferences provides bulletin.notificationPreferences) {
                NotificationSettingsHost { App(appContainer = bulletin.container) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationTap(intent)
        handleCallback(intent)
    }

    override fun onPause() {
        super.onPause()
        leftForBrowser = true
    }

    override fun onResume() {
        super.onResume()
        if (leftForBrowser && BuildConfig.MICROSOFT_SSO_ENABLED) bulletin.externalAuthBrowser.cancel()
        leftForBrowser = false
    }

    private fun handleNotificationTap(intent: Intent) {
        // Try message notification first
        val messageTarget = MessageNotificationIntents.parse(intent)
        if (messageTarget != null) {
            MessageNotificationIntents.consume(intent)
            bulletin.container.openConversationFromNotification(messageTarget.conversationId, messageTarget.recipientId)
            return
        }
        // Try interest notification
        val interestTarget = MarketplaceInterestNotificationIntents.parse(intent)
        if (interestTarget != null) {
            MarketplaceInterestNotificationIntents.consume(intent)
            bulletin.container.openInterestListingFromNotification(interestTarget.listingId, interestTarget.recipientId)
        }
    }

    private fun handleCallback(intent: Intent) {
        // Notification intents carry their target in data; it is not an auth callback.
        if (MessageNotificationIntents.isNotificationIntent(intent)) return
        if (MarketplaceInterestNotificationIntents.isNotificationIntent(intent)) return
        if (BuildConfig.MICROSOFT_SSO_ENABLED) bulletin.externalAuthBrowser.deliver(intent.data)
        intent.data = null
    }

    /** Asks once the user is signed in, when alerts are meaningful, rather than at first launch. */
    private fun requestNotificationPermissionAfterSignIn() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                bulletin.container.isSignedIn.first { it }
                val granted = ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
