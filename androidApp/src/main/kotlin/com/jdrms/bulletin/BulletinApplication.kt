package com.jdrms.bulletin

import android.app.Application
import com.jdrms.bulletin.app.di.AppContainer
import com.jdrms.bulletin.app.di.ExternalAuthConfiguration
import com.jdrms.bulletin.app.di.MarketplacePreferenceStores
import com.jdrms.bulletin.app.notifications.NotificationPreferences
import com.jdrms.bulletin.app.theme.AndroidThemePreferenceStore
import com.jdrms.bulletin.core.network.SupabaseConfig
import com.jdrms.bulletin.domain.marketplace.infrastructure.AndroidMarketplaceLayoutPreferenceStore
import com.jdrms.bulletin.domain.marketplace.infrastructure.AndroidReportedListingVisibilityPreferenceStore
import com.jdrms.bulletin.domain.profile.infrastructure.AndroidAuthStorage
import com.jdrms.bulletin.domain.profile.infrastructure.AndroidExternalAuthBrowser
import com.jdrms.bulletin.notifications.AndroidIncomingMessageNotifier
import com.jdrms.bulletin.notifications.AndroidMarketplaceInterestNotifier

class BulletinApplication : Application() {
    internal val notificationPreferences: NotificationPreferences by lazy {
        val preferences = getSharedPreferences("notification_preferences", MODE_PRIVATE)
        NotificationPreferences(
            initiallyEnabled = preferences.getBoolean("enabled", true),
            persist = { preferences.edit().putBoolean("enabled", it).apply() },
            clearNotifications = {
                messageNotifier.clearAll()
                interestNotifier.clearAll()
            }
        )
    }
    private val authStorage by lazy { AndroidAuthStorage(this, BuildConfig.SUPABASE_URL) }
    internal val messageNotifier by lazy {
        AndroidIncomingMessageNotifier(
            this
        ) { notificationPreferences.enabled.value }
    }
    val externalAuthBrowser by lazy { AndroidExternalAuthBrowser(this, authStorage) }
    internal val interestNotifier by lazy {
        AndroidMarketplaceInterestNotifier(
            this
        ) { notificationPreferences.enabled.value }
    }
    val container by lazy {
        AppContainer(
            supabaseConfig = SupabaseConfig(
                BuildConfig.SUPABASE_URL,
                BuildConfig.SUPABASE_API_KEY,
                BuildConfig.SUPABASE_IS_CONNECTED
            ),
            themePreferenceStore = AndroidThemePreferenceStore(this),
            marketplacePreferences = MarketplacePreferenceStores(
                layout = AndroidMarketplaceLayoutPreferenceStore(this),
                reportedListingVisibility = AndroidReportedListingVisibilityPreferenceStore(this)
            ),
            externalAuth = ExternalAuthConfiguration(
                microsoftSignInEnabled = BuildConfig.MICROSOFT_SSO_ENABLED,
                browser = if (BuildConfig.MICROSOFT_SSO_ENABLED) externalAuthBrowser else null,
                storage = if (BuildConfig.MICROSOFT_SSO_ENABLED) authStorage else null
            ),
            marketplaceInterestNotifier = interestNotifier
        )
    }

    override fun onCreate() {
        super.onCreate()
        interestNotifier.initializeChannel()
    }
}
