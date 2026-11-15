package com.jdrms.bulletin.app.di

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceInterestPreferenceStore
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceLayoutPreferenceStore
import com.jdrms.bulletin.domain.marketplace.application.ReportedListingVisibilityPreferenceStore
import com.jdrms.bulletin.domain.marketplace.infrastructure.InMemoryMarketplaceInterestPreferenceStore
import com.jdrms.bulletin.domain.marketplace.infrastructure.InMemoryMarketplaceLayoutPreferenceStore
import com.jdrms.bulletin.domain.marketplace.infrastructure.InMemoryReportedListingVisibilityPreferenceStore

data class MarketplacePreferenceStores(
    val layout: MarketplaceLayoutPreferenceStore = InMemoryMarketplaceLayoutPreferenceStore(),
    val interests: MarketplaceInterestPreferenceStore = InMemoryMarketplaceInterestPreferenceStore(),
    val reportedListingVisibility: ReportedListingVisibilityPreferenceStore =
        InMemoryReportedListingVisibilityPreferenceStore()
)
