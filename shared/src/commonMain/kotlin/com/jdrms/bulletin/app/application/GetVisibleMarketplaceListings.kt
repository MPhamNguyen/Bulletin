package com.jdrms.bulletin.app.application

import com.jdrms.bulletin.domain.marketplace.application.DEFAULT_MARKETPLACE_PAGE_SIZE
import com.jdrms.bulletin.domain.marketplace.application.EmptyMarketplaceReportedListingProvider
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceBlockedUserProvider
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingPage
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSnapshot
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSource
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageCursor
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageRequest
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceReportedListingProvider
import com.jdrms.bulletin.domain.marketplace.application.toCursor
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceReporterId

/** Shared application policy for which listings a viewer may see. */
class GetVisibleMarketplaceListings(
    private val listingSource: MarketplaceListingSource,
    private val blockedUserProvider: MarketplaceBlockedUserProvider? = null,
    private val reportedListingProvider: MarketplaceReportedListingProvider = EmptyMarketplaceReportedListingProvider
) {
    suspend operator fun invoke(request: MarketplacePageRequest, refresh: Boolean = false): MarketplaceListingPage {
        val hiddenUserIds = request.viewerId?.let { blockedUserProvider?.getHiddenUserIds(it) }.orEmpty()
        val reportedListingIds = request.viewerId
            ?.takeIf(String::isNotBlank)
            ?.let { reportedListingProvider.getReportedListingIds(MarketplaceReporterId(it)) }
            .orEmpty()
        val visible = mutableListOf<MarketplaceListingSnapshot>()
        var cursor = request.cursor
        var usedStaleFallback = false
        var sourceCursor: MarketplacePageCursor?
        do {
            val sourceRequest = request.forVisibilityScan(cursor)
            val page = listingSource.loadAvailableListings(sourceRequest, refresh && cursor == request.cursor)
            usedStaleFallback = usedStaleFallback || page.isStaleFallback
            val allowed = page.listings.filterNot { listing ->
                (request.listingId != null && listing.id != request.listingId) ||
                    listing.sellerId == request.viewerId || listing.sellerId in hiddenUserIds ||
                    MarketplaceItemId(listing.id) in reportedListingIds
            }
            val remaining = request.pageSize - visible.size
            visible += allowed.take(remaining)
            sourceCursor = page.nextCursor
            if (visible.size == request.pageSize) {
                val lastAdded = visible.last()
                val moreAllowedInFetchedPage = allowed.size > remaining
                val nextCursor = if (moreAllowedInFetchedPage) {
                    lastAdded.toCursor()
                } else {
                    sourceCursor
                }
                return MarketplaceListingPage(visible, nextCursor, usedStaleFallback)
            }
            cursor = sourceCursor
        } while (sourceCursor != null)
        return MarketplaceListingPage(visible, null, usedStaleFallback)
    }

    // A targeted visibility check must page through the seller's listings. If the
    // target ID is sent to the source, it can only ever return that one row and
    // cannot establish its position in the filtered listing set.
    private fun MarketplacePageRequest.forVisibilityScan(cursor: MarketplacePageCursor?) = copy(
        cursor = cursor,
        listingId = null,
        pageSize = if (listingId != null) maxOf(pageSize, DEFAULT_MARKETPLACE_PAGE_SIZE) else pageSize
    )
}

private suspend fun MarketplaceListingSource.loadAvailableListings(
    request: MarketplacePageRequest,
    refresh: Boolean
): MarketplaceListingPage = if (refresh) {
    refreshAvailableListings(request)
} else {
    getAvailableListings(request)
}
