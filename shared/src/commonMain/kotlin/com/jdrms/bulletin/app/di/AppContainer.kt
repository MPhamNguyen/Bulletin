package com.jdrms.bulletin.app.di

import com.jdrms.bulletin.app.theme.InMemoryThemePreferenceStore
import com.jdrms.bulletin.app.theme.ThemePreferenceStore
import com.jdrms.bulletin.app.theme.ThemeViewModel
import com.jdrms.bulletin.core.common.FlowUserMessenger
import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.core.network.SupabaseConfig
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceLayoutPreferenceStore
import com.jdrms.bulletin.domain.marketplace.infrastructure.InMemoryMarketplaceLayoutPreferenceStore
import io.github.jan.supabase.SupabaseClient

class AppContainer(
    supabaseConfig: SupabaseConfig = SupabaseConfig(),
    isInspectionMode: Boolean = false,
    allowInMemoryFallback: Boolean = false,
    private val themePreferenceStore: ThemePreferenceStore = InMemoryThemePreferenceStore(),
    marketplaceLayoutPreferenceStore: MarketplaceLayoutPreferenceStore =
        InMemoryMarketplaceLayoutPreferenceStore(),
    providedSupabaseClient: SupabaseClient? = null
) {
    internal val listingChangedSignal by lazy { RefreshSignal() }
    internal val userMessenger by lazy { FlowUserMessenger() }
    private val infrastructure = InfrastructureModule(
        supabaseConfig,
        isInspectionMode,
        allowInMemoryFallback,
        providedSupabaseClient
    )
    internal val profile = ProfileModule(infrastructure)
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
