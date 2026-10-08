package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.application.BookmarkMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.application.GetMarketplaceListingBookmarks
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingPage
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSnapshot
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSource
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageCursor
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageRequest
import com.jdrms.bulletin.domain.marketplace.application.RemoveMarketplaceListingBookmark
import com.jdrms.bulletin.domain.marketplace.application.SearchMarketplace
import com.jdrms.bulletin.domain.marketplace.application.ViewMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.application.pageFor
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.repository.MarketplaceRepository
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryMarketplaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MarketplaceViewModelTestPart1 {

    internal fun uploadedListing(): MarketplaceListingSnapshot {
        return MarketplaceListingSnapshot(
            id = "listing:new",
            sellerId = "seller_new",
            sellerName = "New Seller",
            title = "Graphing Calculator",
            description = "Calculator for math courses",
            priceAmount = 60.0,
            priceCurrency = "USD",
            category = MarketplaceCategory.ELECTRONICS,
            createdAtMillis = 100L
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun duplicateBookmarkTapCreatesOneBookmarkAndDetailReflectsIt() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = InMemoryMarketplaceRepository()
            val viewModel = MarketplaceViewModel(
                searchMarketplace = SearchMarketplace(repository),
                bookmarks = MarketplaceBookmarkDependencies(
                    add = BookmarkMarketplaceListing(repository),
                    remove = RemoveMarketplaceListingBookmark(repository),
                    get = GetMarketplaceListingBookmarks(repository)
                ),
                viewMarketplaceListing = ViewMarketplaceListing(repository)
            )
            advanceUntilIdle()
            val itemId = MarketplaceItemId("mkt_1")

            viewModel.toggleBookmark(itemId)
            viewModel.toggleBookmark(itemId)
            advanceUntilIdle()
            assertEquals(setOf(itemId), repository.getBookmarkedItemIds("student_user"))

            viewModel.onListingClicked(itemId.value)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.selectedListing?.isBookmarked == true)

            viewModel.toggleBookmark(itemId)
            advanceUntilIdle()
            assertEquals(emptySet(), repository.getBookmarkedItemIds("student_user"))
            assertFalse(viewModel.uiState.value.selectedListing?.isBookmarked == true)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun prefixedSupabaseIdSetsBookmarkStateOnDetailedListing() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = RawSupabaseIdMarketplaceRepository()
            val itemId = MarketplaceItemId("listing:listing-42")
            repository.bookmarkListing("student_user", itemId)
            val source = MutableListingSource().apply {
                listings += uploadedListing().copy(id = itemId.value)
            }
            val viewModel = MarketplaceViewModel(
                searchMarketplace = SearchMarketplace(repository, source),
                bookmarks = MarketplaceBookmarkDependencies(
                    add = BookmarkMarketplaceListing(repository),
                    remove = RemoveMarketplaceListingBookmark(repository),
                    get = GetMarketplaceListingBookmarks(repository)
                ),
                viewMarketplaceListing = ViewMarketplaceListing(repository)
            )
            advanceUntilIdle()

            viewModel.onListingClicked(itemId.value)
            advanceUntilIdle()

            assertEquals(itemId, viewModel.uiState.value.selectedListing?.id)
            assertTrue(viewModel.uiState.value.selectedListing?.isBookmarked == true)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun successfulBookmarkStaysVisibleWhenReconciliationRefreshFails() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = BookmarkRefreshFailingRepository()
            val viewModel = MarketplaceViewModel(
                searchMarketplace = SearchMarketplace(repository),
                bookmarks = MarketplaceBookmarkDependencies(
                    add = BookmarkMarketplaceListing(repository),
                    remove = RemoveMarketplaceListingBookmark(repository),
                    get = GetMarketplaceListingBookmarks(repository)
                ),
                viewMarketplaceListing = ViewMarketplaceListing(repository)
            )
            advanceUntilIdle()
            val itemId = MarketplaceItemId("mkt_1")

            viewModel.toggleBookmark(itemId)
            advanceUntilIdle()

            assertEquals(setOf(itemId), viewModel.uiState.value.bookmarkedItemIds)
            assertEquals(setOf(itemId), repository.persistedBookmarks())
            assertEquals(null, viewModel.uiState.value.bookmarkErrorMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun refreshIncludesListingUploadedAfterViewModelWasCreated() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = InMemoryMarketplaceRepository(initialListings = emptyList())
            val source = MutableListingSource()
            val viewModel = MarketplaceViewModel(
                searchMarketplace = SearchMarketplace(repository, source),
                bookmarks = MarketplaceBookmarkDependencies(
                    add = BookmarkMarketplaceListing(repository),
                    remove = RemoveMarketplaceListingBookmark(repository),
                    get = GetMarketplaceListingBookmarks(repository)
                ),
                viewMarketplaceListing = ViewMarketplaceListing(repository)
            )
            advanceUntilIdle()
            assertEquals(emptyList(), viewModel.uiState.value.items)

            source.listings += uploadedListing()
            viewModel.refreshListings()
            advanceUntilIdle()

            assertEquals(listOf("Graphing Calculator"), viewModel.uiState.value.items.map { it.title })

            viewModel.onSearchQueryChanged("electronics")
            advanceUntilIdle()
            assertEquals(listOf("Graphing Calculator"), viewModel.uiState.value.items.map { it.title })
            assertEquals(3, source.loadCount)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun listingChangeSignalRefreshesResultsAndStaleDetail() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = CountingMarketplaceRepository(InMemoryMarketplaceRepository())
            val source = MutableListingSource().apply { listings += uploadedListing().copy(id = "mkt_1") }
            val signal = RefreshSignal()
            val viewModel = MarketplaceViewModel(
                searchMarketplace = SearchMarketplace(repository, source),
                bookmarks = MarketplaceBookmarkDependencies(
                    add = BookmarkMarketplaceListing(repository),
                    remove = RemoveMarketplaceListingBookmark(repository),
                    get = GetMarketplaceListingBookmarks(repository)
                ),
                viewMarketplaceListing = ViewMarketplaceListing(repository),
                listingChangedSignal = signal
            )
            advanceUntilIdle()
            viewModel.onListingClicked("mkt_1")
            advanceUntilIdle()
            val detailLoadsBeforeChange = repository.detailLoadCount

            source.listings[0] = uploadedListing().copy(id = "mkt_1", title = "Updated Calculator")
            repository.detailTitle = "Updated Calculator"
            signal.emit()
            advanceUntilIdle()

            assertTrue(source.loadCount >= 2)
            assertEquals(detailLoadsBeforeChange + 1, repository.detailLoadCount)
            assertEquals("Updated Calculator", viewModel.uiState.value.selectedListing?.title)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun loadFailureStopsLoadingAndExposesRetryableError() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = InMemoryMarketplaceRepository(initialListings = emptyList())
            val viewModel = MarketplaceViewModel(
                searchMarketplace = SearchMarketplace(repository, FailingListingSource),
                bookmarks = MarketplaceBookmarkDependencies(
                    add = BookmarkMarketplaceListing(repository),
                    remove = RemoveMarketplaceListingBookmark(repository),
                    get = GetMarketplaceListingBookmarks(repository)
                ),
                viewMarketplaceListing = ViewMarketplaceListing(repository)
            )

            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertNotNull(viewModel.uiState.value.errorMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun missingListingPreviewStopsLoadingAndExposesTheDetailFailure() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = InMemoryMarketplaceRepository(initialListings = emptyList())
            val viewModel = MarketplaceViewModel(
                searchMarketplace = SearchMarketplace(repository),
                bookmarks = MarketplaceBookmarkDependencies(
                    add = BookmarkMarketplaceListing(repository),
                    remove = RemoveMarketplaceListingBookmark(repository),
                    get = GetMarketplaceListingBookmarks(repository)
                ),
                viewMarketplaceListing = ViewMarketplaceListing(repository)
            )
            advanceUntilIdle()

            viewModel.onListingClicked("missing-listing")
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isDetailSheetOpen)
            assertFalse(viewModel.uiState.value.isDetailLoading)
            assertEquals("Listing not found with ID: missing-listing", viewModel.uiState.value.detailErrorMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun returningFromSellerProfileReopensTheViewedListing() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = InMemoryMarketplaceRepository()
            val viewModel = MarketplaceViewModel(
                searchMarketplace = SearchMarketplace(repository),
                bookmarks = MarketplaceBookmarkDependencies(
                    add = BookmarkMarketplaceListing(repository),
                    remove = RemoveMarketplaceListingBookmark(repository),
                    get = GetMarketplaceListingBookmarks(repository)
                ),
                viewMarketplaceListing = ViewMarketplaceListing(repository)
            )
            advanceUntilIdle()

            viewModel.onListingClicked("mkt_1")
            advanceUntilIdle()
            val viewedListingId = viewModel.uiState.value.selectedListing?.id?.value

            viewModel.onSellerClicked("seller_101")
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isSellerProfileOpen)
            assertFalse(viewModel.uiState.value.isDetailSheetOpen)
            assertEquals(viewedListingId, viewModel.uiState.value.selectedListing?.id?.value)

            viewModel.dismissSellerProfile()

            assertFalse(viewModel.uiState.value.isSellerProfileOpen)
            assertTrue(viewModel.uiState.value.isDetailSheetOpen)
            assertEquals(viewedListingId, viewModel.uiState.value.selectedListing?.id?.value)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun unavailableSellerProfileExposesErrorWithoutFabricatingProfileData() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = InMemoryMarketplaceRepository()
            val viewModel = MarketplaceViewModel(
                searchMarketplace = SearchMarketplace(repository),
                bookmarks = MarketplaceBookmarkDependencies(
                    add = BookmarkMarketplaceListing(repository),
                    remove = RemoveMarketplaceListingBookmark(repository),
                    get = GetMarketplaceListingBookmarks(repository)
                ),
                viewMarketplaceListing = ViewMarketplaceListing(
                    repository,
                    sellerProfileProvider = { null }
                )
            )
            advanceUntilIdle()

            viewModel.onListingClicked("mkt_1")
            advanceUntilIdle()
            viewModel.onSellerClicked("seller_101")
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isSellerProfileLoading)
            assertEquals("Seller profile is currently unavailable.", viewModel.uiState.value.sellerProfileErrorMessage)
            assertEquals(null, viewModel.uiState.value.selectedSellerProfile)
        } finally {
            Dispatchers.resetMain()
        }
    }
}

internal object FailingListingSource : MarketplaceListingSource {
    override suspend fun getAvailableListings(request: MarketplacePageRequest): MarketplaceListingPage {
        error("Database unavailable")
    }
}

internal class MutableListingSource : MarketplaceListingSource {
    val listings = mutableListOf<MarketplaceListingSnapshot>()
    var loadCount = 0

    override suspend fun getAvailableListings(request: MarketplacePageRequest): MarketplaceListingPage {
        loadCount += 1
        return listings.pageFor(request)
    }
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
    private val firstCursor = MarketplacePageCursor(
        createdAt = kotlin.time.Instant.fromEpochMilliseconds(1L),
        itemId = "first-cursor"
    )
    private val secondCursor = MarketplacePageCursor(
        createdAt = kotlin.time.Instant.fromEpochMilliseconds(0L),
        itemId = "second-cursor"
    )

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

    override suspend fun getCatalog(): List<MarketplaceItem> = delegate.getCatalog()

    override suspend fun search(
        query: String,
        category: MarketplaceCategory?
    ): List<MarketplaceItem> = delegate.search(query, category)

    override suspend fun getItem(id: MarketplaceItemId): MarketplaceItem? = delegate.getItem(id)

    override suspend fun viewListing(listingID: String): Result<Listing> {
        detailLoadCount += 1
        return when (val result = delegate.viewListing(listingID)) {
            is Result.Success -> Result.Success(result.data.copy(title = detailTitle))
            is Result.Error -> result
        }
    }

    override suspend fun bookmarkListing(userId: String, itemId: MarketplaceItemId): Result<Unit> =
        delegate.bookmarkListing(userId, itemId)

    override suspend fun removeListingBookmark(userId: String, itemId: MarketplaceItemId): Result<Unit> =
        delegate.removeListingBookmark(userId, itemId)

    override suspend fun getBookmarkedItemIds(userId: String): Set<MarketplaceItemId> =
        delegate.getBookmarkedItemIds(userId)

    override suspend fun getBookmarkedListingsPage(
        userId: String,
        cursor: com.jdrms.bulletin.domain.marketplace.domain.model.ListingBookmarksPageCursor?,
        pageSize: Int
    ): com.jdrms.bulletin.domain.marketplace.domain.model.BookmarkedListingsPage =
        delegate.getBookmarkedListingsPage(userId, cursor, pageSize)
}

private class RawSupabaseIdMarketplaceRepository(
    private val delegate: InMemoryMarketplaceRepository = InMemoryMarketplaceRepository()
) : MarketplaceRepository by delegate {
    override suspend fun viewListing(listingID: String): Result<Listing> {
        return Result.Success(
            Listing(
                id = MarketplaceItemId(listingID.removePrefix("listing:")),
                sellerId = "seller-42",
                sellerName = "Student",
                title = "Campus desk",
                description = "Compact desk for a dorm room.",
                price = com.jdrms.bulletin.domain.marketplace.domain.model.MarketplacePrice(25.0),
                category = MarketplaceCategory.FURNITURE
            )
        )
    }
}

private class BookmarkRefreshFailingRepository(
    private val delegate: InMemoryMarketplaceRepository = InMemoryMarketplaceRepository()
) : MarketplaceRepository by delegate {
    private var failReads = false

    override suspend fun bookmarkListing(userId: String, itemId: MarketplaceItemId): Result<Unit> {
        val result = delegate.bookmarkListing(userId, itemId)
        failReads = true
        return result
    }

    override suspend fun getBookmarkedItemIds(userId: String): Set<MarketplaceItemId> {
        if (failReads) error("Bookmark refresh unavailable")
        return delegate.getBookmarkedItemIds(userId)
    }

    suspend fun persistedBookmarks(): Set<MarketplaceItemId> {
        return delegate.getBookmarkedItemIds("student_user")
    }
}
