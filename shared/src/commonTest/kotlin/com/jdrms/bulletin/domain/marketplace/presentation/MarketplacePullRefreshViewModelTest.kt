package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.application.BookmarkMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.application.GetMarketplaceListingBookmarks
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSnapshot
import com.jdrms.bulletin.domain.marketplace.application.RemoveMarketplaceListingBookmark
import com.jdrms.bulletin.domain.marketplace.application.SearchMarketplace
import com.jdrms.bulletin.domain.marketplace.application.ViewMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.infrastructure.InMemoryMarketplaceLayoutPreferenceStore
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
import kotlin.test.assertTrue

class MarketplacePullRefreshViewModelTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun pullToRefreshExposesProgressAndCompletionState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = InMemoryMarketplaceRepository(initialListings = emptyList())
            val source = MutableListingSource().apply { listings += refreshListing() }
            val viewModel = MarketplaceViewModel(
                layoutPreferenceStore = InMemoryMarketplaceLayoutPreferenceStore(),
                searchMarketplace = SearchMarketplace(repository, source),
                bookmarks = MarketplaceBookmarkDependencies(
                    add = BookmarkMarketplaceListing(repository),
                    remove = RemoveMarketplaceListingBookmark(repository),
                    get = GetMarketplaceListingBookmarks(repository)
                ),
                viewMarketplaceListing = ViewMarketplaceListing(repository)
            )
            advanceUntilIdle()
            source.listings[0] = refreshListing().copy(title = "Updated Calculator")

            viewModel.pullToRefresh()
            testScheduler.runCurrent()

            assertTrue(viewModel.uiState.value.isRefreshing)
            assertTrue(viewModel.uiState.value.refreshComplete)
            assertEquals("Updated Calculator", viewModel.uiState.value.items.single().title)

            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isRefreshing)
            assertFalse(viewModel.uiState.value.refreshComplete)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun refreshListing() = MarketplaceListingSnapshot(
        id = "listing:refresh",
        sellerId = "seller",
        sellerName = "Seller",
        title = "Graphing Calculator",
        description = "Calculator for math courses",
        priceAmount = 60.0,
        priceCurrency = "USD",
        category = MarketplaceCategory.ELECTRONICS,
        createdAtMillis = 100L
    )
}
