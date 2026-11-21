package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingPage
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSource
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageCursor
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageRequest
import com.jdrms.bulletin.domain.marketplace.application.listingComparator
import com.jdrms.bulletin.domain.marketplace.application.toCursor
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class CompositeMarketplaceListingSource(
    private vararg val sources: MarketplaceListingSource
) : MarketplaceListingSource {
    override suspend fun getAvailableListings(request: MarketplacePageRequest): MarketplaceListingPage {
        // Sources are independent, so query them concurrently; awaitAll keeps their declared order.
        val sourcePages = coroutineScope { sources.map { async { it.getAvailableListings(request) } }.awaitAll() }
        val merged = sourcePages
            .flatMap { it.listings }
            .distinctBy { it.id }
            .sortedWith(request.sortOrder.listingComparator)
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
            nextCursor = if (hasMore) {
                pageListings.lastOrNull()?.toCursor(request.sortOrder) ?: sourceCursor
            } else {
                null
            }
        )
    }
}
