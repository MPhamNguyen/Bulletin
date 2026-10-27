package com.jdrms.bulletin

import android.app.Application
import com.jdrms.bulletin.app.di.AppContainer
import com.jdrms.bulletin.app.di.ExternalAuthConfiguration
import com.jdrms.bulletin.app.theme.AndroidThemePreferenceStore
import com.jdrms.bulletin.core.network.SupabaseConfig
import com.jdrms.bulletin.domain.marketplace.infrastructure.AndroidMarketplaceLayoutPreferenceStore
import com.jdrms.bulletin.domain.profile.infrastructure.AndroidAuthStorage
import com.jdrms.bulletin.domain.profile.infrastructure.AndroidExternalAuthBrowser

class BulletinApplication : Application() {
    private val authStorage by lazy { AndroidAuthStorage(this, BuildConfig.SUPABASE_URL) }
    val externalAuthBrowser by lazy { AndroidExternalAuthBrowser(this, authStorage) }
    val container by lazy {
        AppContainer(
            supabaseConfig = SupabaseConfig(
                BuildConfig.SUPABASE_URL,
                BuildConfig.SUPABASE_API_KEY,
                BuildConfig.SUPABASE_IS_CONNECTED
            ),
            themePreferenceStore = AndroidThemePreferenceStore(this),
            marketplaceLayoutPreferenceStore = AndroidMarketplaceLayoutPreferenceStore(this),
            externalAuth = ExternalAuthConfiguration(
                microsoftSignInEnabled = BuildConfig.MICROSOFT_SSO_ENABLED,
                browser = externalAuthBrowser,
                storage = authStorage
            )
        )
    }
}
