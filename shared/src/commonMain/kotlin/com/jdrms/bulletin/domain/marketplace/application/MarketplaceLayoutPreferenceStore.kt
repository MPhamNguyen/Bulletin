package com.jdrms.bulletin.domain.marketplace.application

enum class MarketplaceLayoutPreference(val columns: Int) {
    ONE_PER_ROW(1),
    TWO_PER_ROW(2)
}

interface MarketplaceLayoutPreferenceStore {
    suspend fun get(): MarketplaceLayoutPreference

    suspend fun set(preference: MarketplaceLayoutPreference)
}
