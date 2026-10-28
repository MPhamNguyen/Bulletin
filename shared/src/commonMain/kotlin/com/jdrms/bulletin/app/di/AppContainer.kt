package com.jdrms.bulletin.app.di

import com.jdrms.bulletin.app.theme.InMemoryThemePreferenceStore
import com.jdrms.bulletin.app.theme.ThemePreferenceStore
import com.jdrms.bulletin.app.theme.ThemeViewModel
import com.jdrms.bulletin.core.common.FlowUserMessenger
import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.core.network.SupabaseAuthStorage
import com.jdrms.bulletin.core.network.SupabaseConfig
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceLayoutPreferenceStore
import com.jdrms.bulletin.domain.marketplace.infrastructure.InMemoryMarketplaceLayoutPreferenceStore
import com.jdrms.bulletin.domain.profile.infrastructure.repository.ExternalAuthBrowser
import io.github.jan.supabase.SupabaseClient

class AppContainer(
    supabaseConfig: SupabaseConfig = SupabaseConfig(),
    isInspectionMode: Boolean = false,
    allowInMemoryFallback: Boolean = false,
    private val themePreferenceStore: ThemePreferenceStore = InMemoryThemePreferenceStore(),
    marketplaceLayoutPreferenceStore: MarketplaceLayoutPreferenceStore =
        InMemoryMarketplaceLayoutPreferenceStore(),
    providedSupabaseClient: SupabaseClient? = null,
    private val externalAuth: ExternalAuthConfiguration = ExternalAuthConfiguration()
) {
    val microsoftSignInEnabled: Boolean get() = externalAuth.microsoftSignInEnabled
    val externalSignInPending: Boolean get() = externalAuth.browser?.isResuming == true

    internal val listingChangedSignal by lazy { RefreshSignal() }
    internal val userMessenger by lazy { FlowUserMessenger() }
    private val infrastructure = InfrastructureModule(
        supabaseConfig,
        isInspectionMode,
        allowInMemoryFallback,
        providedSupabaseClient,
        externalAuth.storage,
        externalAuth.microsoftSignInEnabled
    )
    internal val profile = ProfileModule(infrastructure, externalAuth.browser)
    internal val listings: ListingsModule = ListingsModule(infrastructure, profile) { marketplace.invalidateReads() }
    internal val marketplace: MarketplaceModule = MarketplaceModule(
        infrastructure,
        profile,
        listings,
        marketplaceLayoutPreferenceStore
    )
    internal val messages = MessagesModule(infrastructure, profile, listings, marketplace)
    internal val home = HomeModule(marketplace, messages)

    internal fun createThemeViewModel() = ThemeViewModel(themePreferenceStore)
}

data class ExternalAuthConfiguration(
    val microsoftSignInEnabled: Boolean = false,
    val browser: ExternalAuthBrowser? = null,
    val storage: SupabaseAuthStorage? = null
)
