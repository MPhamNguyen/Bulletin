package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingPage
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSource
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageCursor
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageRequest
import com.jdrms.bulletin.domain.marketplace.application.marketplaceListingComparator
import com.jdrms.bulletin.domain.marketplace.application.toCursor

class CompositeMarketplaceListingSource(
    private vararg val sources: MarketplaceListingSource
) : MarketplaceListingSource {
    override suspend fun getAvailableListings(request: MarketplacePageRequest): MarketplaceListingPage {
        val sourcePages = sources.map { it.getAvailableListings(request) }
        val merged = sourcePages
            .flatMap { it.listings }
            .distinctBy { it.id }
            .sortedWith(marketplaceListingComparator)
        val pageListings = merged.take(request.pageSize)
        val hasMore = merged.size > request.pageSize || sourcePages.any { it.nextCursor != null }
        val sourceCursor = sourcePages.mapNotNull { it.nextCursor }
            .filterNot { it == request.cursor }
            .maxWithOrNull(
                compareBy<MarketplacePageCursor> { it.createdAt }
                    .thenBy { it.itemId }
            )
        return MarketplaceListingPage(
            listings = pageListings,
            nextCursor = if (hasMore) pageListings.lastOrNull()?.toCursor() ?: sourceCursor else null
        )
    }
}
