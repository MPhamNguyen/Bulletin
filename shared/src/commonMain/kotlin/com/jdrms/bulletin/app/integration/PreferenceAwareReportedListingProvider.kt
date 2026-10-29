package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceReportedListingProvider
import com.jdrms.bulletin.domain.marketplace.application.ReportedListingVisibilityPreferenceStore
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceReporterId

class PreferenceAwareReportedListingProvider(
    private val delegate: MarketplaceReportedListingProvider,
    private val preferenceStore: ReportedListingVisibilityPreferenceStore
) : MarketplaceReportedListingProvider {
    override suspend fun getReportedListingIds(reporterId: MarketplaceReporterId) =
        if (preferenceStore.shouldHideReportedListings(reporterId)) {
            delegate.getReportedListingIds(reporterId)
        } else {
            emptySet()
        }
}
