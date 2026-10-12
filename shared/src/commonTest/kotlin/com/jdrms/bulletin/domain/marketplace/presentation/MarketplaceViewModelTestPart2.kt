package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.application.BookmarkMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.application.GetMarketplaceListingBookmarks
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSnapshot
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSource
import com.jdrms.bulletin.domain.marketplace.application.RemoveMarketplaceListingBookmark
import com.jdrms.bulletin.domain.marketplace.application.SearchMarketplace
import com.jdrms.bulletin.domain.marketplace.application.ViewMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
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

class MarketplaceViewModelTestPart2 {

    private fun uploadedListing(): MarketplaceListingSnapshot {
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

    private fun createViewModel(
        repository: InMemoryMarketplaceRepository,
        source: MarketplaceListingSource
    ) = MarketplaceViewModel(
        searchMarketplace = SearchMarketplace(repository, source),
        bookmarks = MarketplaceBookmarkDependencies(
            add = BookmarkMarketplaceListing(repository),
            remove = RemoveMarketplaceListingBookmark(repository),
            get = GetMarketplaceListingBookmarks(repository)
        ),
        viewMarketplaceListing = ViewMarketplaceListing(repository)
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun loadNextPageAppendsListingsAndStopsAtTheEnd() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = InMemoryMarketplaceRepository(initialListings = emptyList())
            val source = MutableListingSource().apply {
                listings += (1L..21L).map { index ->
                    uploadedListing().copy(
                        id = "listing:$index",
                        title = "Listing $index",
                        createdAtMillis = index
                    )
                }
            }
            val viewModel = createViewModel(repository, source)
            advanceUntilIdle()

            assertEquals(20, viewModel.uiState.value.items.size)
            assertFalse(viewModel.uiState.value.endReached)

            viewModel.loadNextPage()
            viewModel.loadNextPage()
            advanceUntilIdle()

            assertEquals(21, viewModel.uiState.value.items.size)
            assertTrue(viewModel.uiState.value.endReached)
            assertEquals(2, source.loadCount)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun nextPageFailureKeepsListingsAndCanBeRetried() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = InMemoryMarketplaceRepository(initialListings = emptyList())
            val source = RetryableNextPageSource(
                listings = (1L..21L).map { index ->
                    uploadedListing().copy(
                        id = "listing:$index",
                        title = "Listing $index",
                        createdAtMillis = index
                    )
                }
            )
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

            viewModel.loadNextPage()
            advanceUntilIdle()

            assertEquals(20, viewModel.uiState.value.items.size)
            assertNotNull(viewModel.uiState.value.errorMessage)
            assertNotNull(viewModel.uiState.value.nextCursor)

            viewModel.retryListings()
            advanceUntilIdle()

            assertEquals(21, viewModel.uiState.value.items.size)
            assertTrue(viewModel.uiState.value.endReached)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun emptyIntermediatePageKeepsItsContinuationCursor() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = InMemoryMarketplaceRepository(initialListings = emptyList())
            val source = EmptyIntermediatePageSource(
                firstPageListings = (1L..20L).map { index ->
                    uploadedListing().copy(
                        id = "listing:$index",
                        title = "Listing $index",
                        createdAtMillis = index
                    )
                },
                finalListing = uploadedListing().copy(
                    id = "listing:final",
                    title = "Final listing",
                    createdAtMillis = 0L
                )
            )
            val viewModel = createViewModel(repository, source)
            advanceUntilIdle()

            viewModel.loadNextPage()
            advanceUntilIdle()

            assertEquals(20, viewModel.uiState.value.items.size)
            assertNotNull(viewModel.uiState.value.nextCursor)
            assertFalse(viewModel.uiState.value.endReached)

            viewModel.loadNextPage()
            advanceUntilIdle()

            assertEquals(21, viewModel.uiState.value.items.size)
            assertEquals("Final listing", viewModel.uiState.value.items.last().title)
            assertTrue(viewModel.uiState.value.endReached)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun categoryAndKeywordsRemainAppliedAcrossEveryPage() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = InMemoryMarketplaceRepository(initialListings = emptyList())
            val source = MutableListingSource().apply {
                listings += (1L..25L).map { index ->
                    uploadedListing().copy(
                        id = "listing:textbook-$index",
                        title = "Calculus Textbook $index",
                        category = MarketplaceCategory.TEXTBOOKS,
                        createdAtMillis = index
                    )
                }
                listings += (26L..50L).map { index ->
                    uploadedListing().copy(
                        id = "listing:electronics-$index",
                        title = "Calculus Calculator $index",
                        category = MarketplaceCategory.ELECTRONICS,
                        createdAtMillis = index
                    )
                }
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

            viewModel.onCategorySelected(MarketplaceCategory.TEXTBOOKS)
            viewModel.onSearchQueryChanged("c")
            advanceUntilIdle()

            assertEquals(20, viewModel.uiState.value.items.size)
            assertTrue(viewModel.uiState.value.items.all { it.category == MarketplaceCategory.TEXTBOOKS })
            assertFalse(viewModel.uiState.value.endReached)

            viewModel.loadNextPage()
            advanceUntilIdle()

            assertEquals(25, viewModel.uiState.value.items.size)
            val allItemsMatch = viewModel.uiState.value.items.all { item ->
                item.category == MarketplaceCategory.TEXTBOOKS &&
                    item.title.contains("c", ignoreCase = true)
            }
            assertTrue(allItemsMatch)
            assertTrue(viewModel.uiState.value.endReached)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun everyCategoryPaginatesWithTheSameAppendBehaviorAsAll() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = InMemoryMarketplaceRepository(initialListings = emptyList())
            val source = MutableListingSource().apply {
                MarketplaceCategory.entries.forEachIndexed { categoryIndex, category ->
                    listings += (1L..21L).map { listingIndex ->
                        val sequence = categoryIndex * 100L + listingIndex
                        uploadedListing().copy(
                            id = "listing:${category.name.lowercase()}-$listingIndex",
                            title = "${category.name} Listing $listingIndex",
                            category = category,
                            createdAtMillis = sequence
                        )
                    }
                }
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

            assertEquals(20, viewModel.uiState.value.items.size)
            viewModel.loadNextPage()
            advanceUntilIdle()
            assertEquals(40, viewModel.uiState.value.items.size)

            MarketplaceCategory.entries.forEach { category ->
                viewModel.onCategorySelected(category)
                advanceUntilIdle()

                assertEquals(20, viewModel.uiState.value.items.size, category.name)
                assertTrue(viewModel.uiState.value.items.all { it.category == category }, category.name)
                assertFalse(viewModel.uiState.value.endReached, category.name)

                viewModel.loadNextPage()
                advanceUntilIdle()

                assertEquals(21, viewModel.uiState.value.items.size, category.name)
                assertTrue(viewModel.uiState.value.items.all { it.category == category }, category.name)
                assertTrue(viewModel.uiState.value.endReached, category.name)
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun refreshingACancelledRequestDoesNotExposeItsFailure() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = InMemoryMarketplaceRepository(initialListings = emptyList())
            val source = CancellableFirstRequestSource(uploadedListing())
            val viewModel = MarketplaceViewModel(
                searchMarketplace = SearchMarketplace(repository, source),
                bookmarks = MarketplaceBookmarkDependencies(
                    add = BookmarkMarketplaceListing(repository),
                    remove = RemoveMarketplaceListingBookmark(repository),
                    get = GetMarketplaceListingBookmarks(repository)
                ),
                viewMarketplaceListing = ViewMarketplaceListing(repository)
            )
            testScheduler.runCurrent()

            viewModel.refreshListings()
            advanceUntilIdle()

            assertEquals(listOf("Graphing Calculator"), viewModel.uiState.value.items.map { it.title })
            assertEquals(null, viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isLoading)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
