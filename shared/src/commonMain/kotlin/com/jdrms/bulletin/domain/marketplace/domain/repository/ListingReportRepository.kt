package com.jdrms.bulletin.domain.marketplace.domain.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.ListingReport
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceReporterId

interface ListingReportRepository {
    suspend fun submit(report: ListingReport): Result<Unit>

    suspend fun hasOpenReport(
        reporterId: MarketplaceReporterId,
        listingId: MarketplaceItemId
    ): Result<Boolean>

    suspend fun reportedListingIds(reporterId: MarketplaceReporterId): Result<Set<MarketplaceItemId>>
}
