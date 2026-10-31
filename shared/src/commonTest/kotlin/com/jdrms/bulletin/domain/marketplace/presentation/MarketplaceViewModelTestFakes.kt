package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.application.*
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplacePrice
import com.jdrms.bulletin.domain.marketplace.domain.repository.MarketplaceRepository
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryMarketplaceRepository
import kotlinx.coroutines.awaitCancellation

internal object FailingListingSource : MarketplaceListingSource {
    override suspend fun getAvailableListings(request: MarketplacePageRequest): MarketplaceListingPage =
        error("Database unavailable")
}

internal class MutableListingSource : MarketplaceListingSource {
    val listings = mutableListOf<MarketplaceListingSnapshot>()
    var loadCount = 0
    var staleOnRefresh = false

    override suspend fun getAvailableListings(request: MarketplacePageRequest): MarketplaceListingPage {
        loadCount += 1
        return listings.pageFor(request)
    }

    override suspend fun refreshAvailableListings(request: MarketplacePageRequest): MarketplaceListingPage =
        getAvailableListings(request).copy(isStaleFallback = staleOnRefresh)
}

internal class RetryableNextPageSource(
    private val listings: List<MarketplaceListingSnapshot>
) : MarketplaceListingSource {
    private var shouldFailNextPage = true

    override suspend fun getAvailableListings(request: MarketplacePageRequest): MarketplaceListingPage {
        if (request.cursor != null && shouldFailNextPage) {
            shouldFailNextPage = false
            error("Temporary database failure")
        }
        return listings.pageFor(request)
    }
}

internal class EmptyIntermediatePageSource(
    private val firstPageListings: List<MarketplaceListingSnapshot>,
    private val finalListing: MarketplaceListingSnapshot
) : MarketplaceListingSource {
    private val firstCursor = MarketplacePageCursor(kotlin.time.Instant.fromEpochMilliseconds(1L), "first-cursor")
    private val secondCursor = MarketplacePageCursor(kotlin.time.Instant.fromEpochMilliseconds(0L), "second-cursor")

    override suspend fun getAvailableListings(request: MarketplacePageRequest): MarketplaceListingPage {
        return when (request.cursor) {
            null -> MarketplaceListingPage(firstPageListings, firstCursor)
            firstCursor -> MarketplaceListingPage(emptyList(), secondCursor)
            secondCursor -> MarketplaceListingPage(listOf(finalListing), null)
            else -> error("Unexpected page cursor: ${request.cursor}")
        }
    }
}

internal class CancellableFirstRequestSource(
    private val listing: MarketplaceListingSnapshot
) : MarketplaceListingSource {
    private var requestCount = 0

    override suspend fun getAvailableListings(request: MarketplacePageRequest): MarketplaceListingPage {
        requestCount += 1
        if (requestCount == 1) awaitCancellation()
        return listOf(listing).pageFor(request)
    }
}

internal class CountingMarketplaceRepository(
    private val delegate: MarketplaceRepository
) : MarketplaceRepository {
    var detailLoadCount = 0
    var detailTitle = "Graphing Calculator"
    var bookmarkMutationCount = 0

    override suspend fun getCatalog(): List<MarketplaceItem> = delegate.getCatalog()
    override suspend fun search(
        query: String,
        category: MarketplaceCategory?,
        condition: com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCondition?
    ): List<MarketplaceItem> = delegate.search(query, category, condition)
    override suspend fun getItem(id: MarketplaceItemId): MarketplaceItem? = delegate.getItem(id)
    override suspend fun viewListing(listingID: String): Result<Listing> {
        detailLoadCount += 1
        return when (val result = delegate.viewListing(listingID)) {
            is Result.Success -> Result.Success(result.data.copy(title = detailTitle))
            is Result.Error -> result
        }
    }
    override suspend fun bookmarkListing(userId: String, itemId: MarketplaceItemId): Result<Unit> {
        bookmarkMutationCount += 1
        return delegate.bookmarkListing(userId, itemId)
    }
    override suspend fun removeListingBookmark(userId: String, itemId: MarketplaceItemId): Result<Unit> {
        bookmarkMutationCount += 1
        return delegate.removeListingBookmark(userId, itemId)
    }
    override suspend fun getBookmarkedItemIds(userId: String): Set<MarketplaceItemId> =
        delegate.getBookmarkedItemIds(userId)
    override suspend fun getBookmarkedListingsPage(
        userId: String,
        cursor: com.jdrms.bulletin.domain.marketplace.domain.model.ListingBookmarksPageCursor?,
        pageSize: Int
    ): com.jdrms.bulletin.domain.marketplace.domain.model.BookmarkedListingsPage =
        delegate.getBookmarkedListingsPage(userId, cursor, pageSize)
}

internal class RawSupabaseIdMarketplaceRepository(
    private val delegate: InMemoryMarketplaceRepository = InMemoryMarketplaceRepository()
) : MarketplaceRepository by delegate {
    override suspend fun viewListing(listingID: String): Result<Listing> = Result.Success(
        Listing(
            id = MarketplaceItemId(listingID.removePrefix("listing:")),
            sellerId = "seller-42",
            sellerName = "Student",
            title = "Campus desk",
            description = "Compact desk for a dorm room.",
            price = MarketplacePrice(25.0),
            category = MarketplaceCategory.FURNITURE
        )
    )
}

internal class BookmarkRefreshFailingRepository(
    private val delegate: InMemoryMarketplaceRepository = InMemoryMarketplaceRepository()
) : MarketplaceRepository by delegate {
    private var failReads = false
    var bookmarkReadCount = 0

    override suspend fun bookmarkListing(userId: String, itemId: MarketplaceItemId): Result<Unit> {
        val result = delegate.bookmarkListing(userId, itemId)
        failReads = true
        return result
    }
    override suspend fun getBookmarkedItemIds(userId: String): Set<MarketplaceItemId> {
        bookmarkReadCount += 1
        if (failReads) error("Bookmark refresh unavailable")
        return delegate.getBookmarkedItemIds(userId)
    }
    suspend fun persistedBookmarks(): Set<MarketplaceItemId> = delegate.getBookmarkedItemIds("student_user")
}
