package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.domain.marketplace.application.BookmarkMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.application.GetMarketplaceListingBookmarks
import com.jdrms.bulletin.domain.marketplace.application.InMemoryMarketplaceLayoutPreferenceStore
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceLayoutPreference
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceLayoutPreferenceStore
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSnapshot
import com.jdrms.bulletin.domain.marketplace.application.RemoveMarketplaceListingBookmark
import com.jdrms.bulletin.domain.marketplace.application.SearchMarketplace
import com.jdrms.bulletin.domain.marketplace.application.ViewMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.repository.MarketplaceRepository
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryMarketplaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun layoutPreferenceSurvivesViewModelRecreationWithoutAnAccount() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = InMemoryMarketplaceRepository()
            val store = InMemoryMarketplaceLayoutPreferenceStore()
            val firstViewModel = marketplaceViewModel(repository, store) { null }
            advanceUntilIdle()

            firstViewModel.onLayoutPreferenceChanged(MarketplaceLayoutPreference.ONE_PER_ROW)
            advanceUntilIdle()
            val recreatedViewModel = marketplaceViewModel(repository, store) { null }
            advanceUntilIdle()

            assertEquals(
                MarketplaceLayoutPreference.ONE_PER_ROW,
                recreatedViewModel.uiState.value.layoutPreference
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun failedLayoutPreferenceWriteRestoresPersistedPreference() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val store = object : MarketplaceLayoutPreferenceStore {
                override suspend fun get() = MarketplaceLayoutPreference.TWO_PER_ROW

                override suspend fun set(preference: MarketplaceLayoutPreference) {
                    error("Preference storage unavailable")
                }
            }
            val viewModel = marketplaceViewModel(InMemoryMarketplaceRepository(), store) { null }
            advanceUntilIdle()

            viewModel.onLayoutPreferenceChanged(MarketplaceLayoutPreference.ONE_PER_ROW)
            advanceUntilIdle()

            assertEquals(MarketplaceLayoutPreference.TWO_PER_ROW, viewModel.uiState.value.layoutPreference)
        } finally {
            Dispatchers.resetMain()
        }
    }

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
    fun rapidBookmarkTogglesPersistOnlyTheFinalState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = CountingMarketplaceRepository(InMemoryMarketplaceRepository())
            val viewModel = marketplaceViewModel(repository)
            advanceUntilIdle()
            val itemId = MarketplaceItemId("mkt_1")

            viewModel.toggleBookmark(itemId)
            viewModel.toggleBookmark(itemId)
            advanceUntilIdle()
            assertEquals(emptySet(), repository.getBookmarkedItemIds("student_user"))
            assertEquals(0, repository.bookmarkMutationCount)

            viewModel.toggleBookmark(itemId)
            viewModel.toggleBookmark(itemId)
            viewModel.toggleBookmark(itemId)
            advanceUntilIdle()
            assertEquals(setOf(itemId), repository.getBookmarkedItemIds("student_user"))
            assertEquals(1, repository.bookmarkMutationCount)

            viewModel.onListingClicked(itemId.value)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.detail.listing?.isBookmarked == true)

            viewModel.toggleBookmark(itemId)
            advanceUntilIdle()
            assertEquals(emptySet(), repository.getBookmarkedItemIds("student_user"))
            assertEquals(2, repository.bookmarkMutationCount)
            assertFalse(viewModel.uiState.value.detail.listing?.isBookmarked == true)
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
                viewMarketplaceListing = ViewMarketplaceListing(repository),
                currentUserIdProvider = { "student_user" }
            )
            advanceUntilIdle()

            viewModel.onListingClicked(itemId.value)
            advanceUntilIdle()

            assertEquals(itemId, viewModel.uiState.value.detail.listing?.id)
            assertTrue(viewModel.uiState.value.detail.listing?.isBookmarked == true)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun successfulBookmarkDoesNotTriggerAReconciliationRead() = runTest {
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
                viewMarketplaceListing = ViewMarketplaceListing(repository),
                currentUserIdProvider = { "student_user" }
            )
            advanceUntilIdle()
            val itemId = MarketplaceItemId("mkt_1")

            viewModel.toggleBookmark(itemId)
            advanceUntilIdle()

            assertEquals(setOf(itemId), viewModel.uiState.value.bookmarkedItemIds)
            assertEquals(setOf(itemId), repository.persistedBookmarks())
            assertEquals(1, repository.bookmarkReadCount)
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
            assertEquals("Updated Calculator", viewModel.uiState.value.detail.listing?.title)
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

            assertTrue(viewModel.uiState.value.detail.isOpen)
            assertFalse(viewModel.uiState.value.detail.isLoading)
            assertEquals("Listing not found with ID: missing-listing", viewModel.uiState.value.detail.errorMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }
}

private fun marketplaceViewModel(
    repository: MarketplaceRepository,
    layoutPreferenceStore: MarketplaceLayoutPreferenceStore = InMemoryMarketplaceLayoutPreferenceStore(),
    currentUserIdProvider: suspend () -> String? = { "student_user" }
) = MarketplaceViewModel(
    searchMarketplace = SearchMarketplace(repository),
    bookmarks = MarketplaceBookmarkDependencies(
        add = BookmarkMarketplaceListing(repository),
        remove = RemoveMarketplaceListingBookmark(repository),
        get = GetMarketplaceListingBookmarks(repository)
    ),
    viewMarketplaceListing = ViewMarketplaceListing(repository),
    currentUserIdProvider = currentUserIdProvider,
    layoutPreferenceStore = layoutPreferenceStore
)
