package com.jdrms.bulletin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.jdrms.bulletin.app.App
import com.jdrms.bulletin.app.di.AppContainer
import com.jdrms.bulletin.core.network.SupabaseConfig

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val supabaseConfig = if (BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_API_KEY.isNotBlank()) {
            SupabaseConfig(
                url = BuildConfig.SUPABASE_URL,
                apiKey = BuildConfig.SUPABASE_API_KEY,
                isConnected = BuildConfig.SUPABASE_IS_CONNECTED
            )
        } else {
            SupabaseConfig()
        }

        val appContainer = AppContainer(supabaseConfig = supabaseConfig)

        setContent {
            App(appContainer = appContainer)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
