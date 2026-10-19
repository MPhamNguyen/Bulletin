package com.jdrms.bulletin.domain.marketplace.application

enum class MarketplaceLayoutPreference(val columns: Int) {
    ONE_PER_ROW(1),
    TWO_PER_ROW(2),
    THREE_PER_ROW(3)
}

interface MarketplaceLayoutPreferenceStore {
    suspend fun get(): MarketplaceLayoutPreference

    suspend fun set(preference: MarketplaceLayoutPreference)
}

class InMemoryMarketplaceLayoutPreferenceStore(
    initialPreference: MarketplaceLayoutPreference = MarketplaceLayoutPreference.ONE_PER_ROW
) : MarketplaceLayoutPreferenceStore {
    private var preference = initialPreference

    override suspend fun get(): MarketplaceLayoutPreference = preference

    override suspend fun set(preference: MarketplaceLayoutPreference) {
        this.preference = preference
    }
}
