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
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Shared application policy for which listings a viewer may see. */
class GetVisibleMarketplaceListings(
    private val listingSource: MarketplaceListingSource,
    private val blockedUserProvider: MarketplaceBlockedUserProvider? = null,
    private val reportedListingProvider: MarketplaceReportedListingProvider = EmptyMarketplaceReportedListingProvider
) {
    /**
     * The blocked-user lookup, the reported-listing lookup and the first source page do not depend on one
     * another, so they run concurrently; the first page is only filtered once both lookups have returned.
     * Any failure cancels the remaining work and propagates, as it did when these ran in sequence.
     */
    suspend operator fun invoke(request: MarketplacePageRequest): MarketplaceListingPage = coroutineScope {
        val hiddenLookup = async { loadHiddenUserIds(request) }
        val reportedLookup = async { loadReportedListingIds(request) }
        var firstPage: Deferred<MarketplaceListingPage>? = async {
            listingSource.getAvailableListings(request.forVisibilityScan(request.cursor))
        }
        val hiddenUserIds = hiddenLookup.await()
        val reportedListingIds = reportedLookup.await()
        val visible = mutableListOf<MarketplaceListingSnapshot>()
        var cursor = request.cursor
        var sourceCursor: MarketplacePageCursor?
        do {
            val page = firstPage?.await() ?: listingSource.getAvailableListings(request.forVisibilityScan(cursor))
            firstPage = null
            val allowed = page.listings.filter { isVisible(request, it, hiddenUserIds, reportedListingIds) }
            val remaining = request.pageSize - visible.size
            visible += allowed.take(remaining)
            sourceCursor = page.nextCursor
            if (visible.size == request.pageSize) {
                val hasMoreInFetchedPage = allowed.size > remaining
                val nextCursor = if (hasMoreInFetchedPage) visible.last().toCursor(request.sortOrder) else sourceCursor
                return@coroutineScope MarketplaceListingPage(visible, nextCursor)
            }
            cursor = sourceCursor
        } while (sourceCursor != null)
        MarketplaceListingPage(visible, null)
    }

    private suspend fun loadHiddenUserIds(request: MarketplacePageRequest): Set<String> =
        request.viewerId?.let { blockedUserProvider?.getHiddenUserIds(it) }.orEmpty()

    private suspend fun loadReportedListingIds(request: MarketplacePageRequest): Set<MarketplaceItemId> =
        request.viewerId
            ?.takeIf(String::isNotBlank)
            ?.let { reportedListingProvider.getReportedListingIds(MarketplaceReporterId(it)) }
            .orEmpty()

    private fun isVisible(
        request: MarketplacePageRequest,
        listing: MarketplaceListingSnapshot,
        hiddenUserIds: Set<String>,
        reportedListingIds: Set<MarketplaceItemId>
    ): Boolean = !(
        (request.listingId != null && listing.id != request.listingId) ||
            listing.sellerId == request.viewerId || listing.sellerId in hiddenUserIds ||
            MarketplaceItemId(listing.id) in reportedListingIds
        )

    // A targeted visibility check must page through the seller's listings. If the
    // target ID is sent to the source, it can only ever return that one row and
    // cannot establish its position in the filtered listing set.
    private fun MarketplacePageRequest.forVisibilityScan(cursor: MarketplacePageCursor?) = copy(
        cursor = cursor,
        listingId = null,
        pageSize = if (listingId != null) maxOf(pageSize, DEFAULT_MARKETPLACE_PAGE_SIZE) else pageSize
    )
}
