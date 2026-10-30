package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceReporterId

interface ReportedListingVisibilityPreferenceStore {
    suspend fun shouldHideReportedListings(reporterId: MarketplaceReporterId): Boolean

    suspend fun setHideReportedListings(reporterId: MarketplaceReporterId, shouldHide: Boolean)
}
