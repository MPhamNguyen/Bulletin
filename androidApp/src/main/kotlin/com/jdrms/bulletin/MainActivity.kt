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
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.jdrms.bulletin.app.App
import com.jdrms.bulletin.notifications.MarketplaceInterestNotificationIntents
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val bulletin get() = application as BulletinApplication
    private var leftForBrowser = false
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Interest alerts remain off if permission is denied. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleNotificationTap(intent)
        handleCallback(intent)
        bulletin.container.startMarketplaceInterestNotifications()
        requestNotificationPermissionAfterSignIn()
        setContent { App(appContainer = bulletin.container) }
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

    private fun handleCallback(intent: Intent) {
        if (MarketplaceInterestNotificationIntents.isNotificationIntent(intent)) return
        if (BuildConfig.MICROSOFT_SSO_ENABLED) bulletin.externalAuthBrowser.deliver(intent.data)
        intent.data = null
    }

    private fun handleNotificationTap(intent: Intent) {
        val target = MarketplaceInterestNotificationIntents.parse(intent) ?: return
        MarketplaceInterestNotificationIntents.consume(intent)
        bulletin.container.openInterestListingFromNotification(target.listingId, target.recipientId)
    }

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
