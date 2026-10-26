package com.jdrms.bulletin.domain.marketplace.infrastructure

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceLayoutPreference
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceLayoutPreferenceStore

class InMemoryMarketplaceLayoutPreferenceStore(
    initialPreference: MarketplaceLayoutPreference = MarketplaceLayoutPreference.TWO_PER_ROW
) : MarketplaceLayoutPreferenceStore {
    private var preference = initialPreference

    override suspend fun get(): MarketplaceLayoutPreference = preference

    override suspend fun set(preference: MarketplaceLayoutPreference) {
        this.preference = preference
    }
}
