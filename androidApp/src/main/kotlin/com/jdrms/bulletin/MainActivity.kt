package com.jdrms.bulletin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.jdrms.bulletin.app.App

class MainActivity : ComponentActivity() {
    private val bulletin get() = application as BulletinApplication
    private var leftForBrowser = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleCallback(intent)
        setContent { App(appContainer = bulletin.container) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
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
        if (BuildConfig.MICROSOFT_SSO_ENABLED) bulletin.externalAuthBrowser.deliver(intent.data)
        intent.data = null
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
