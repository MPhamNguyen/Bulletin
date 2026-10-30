package com.jdrms.bulletin.domain.marketplace.infrastructure

import com.jdrms.bulletin.domain.marketplace.application.ReportedListingVisibilityPreferenceStore
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceReporterId

class InMemoryReportedListingVisibilityPreferenceStore : ReportedListingVisibilityPreferenceStore {
    private val preferences = mutableMapOf<MarketplaceReporterId, Boolean>()

    override suspend fun shouldHideReportedListings(reporterId: MarketplaceReporterId): Boolean =
        preferences[reporterId] ?: true

    override suspend fun setHideReportedListings(reporterId: MarketplaceReporterId, shouldHide: Boolean) {
        preferences[reporterId] = shouldHide
    }
}
