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

class MarketplaceRefreshTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun refreshLoadsNewListingsAndReportsWhenItHadToUseCachedResults() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = InMemoryMarketplaceRepository(initialListings = emptyList())
            val source = MutableListingSource()
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
            assertEquals(emptyList(), viewModel.uiState.value.items)

            source.listings += marketplaceListing()
            source.staleOnRefresh = true
            viewModel.refreshListings()
            advanceUntilIdle()

            assertEquals(listOf("Graphing Calculator"), viewModel.uiState.value.items.map { it.title })
            assertEquals(
                "An Error Occured",
                viewModel.uiState.value.refreshNotice
            )

            source.staleOnRefresh = false
            viewModel.onSearchQueryChanged("electronics")
            advanceUntilIdle()

            assertEquals(listOf("Graphing Calculator"), viewModel.uiState.value.items.map { it.title })
            assertEquals(null, viewModel.uiState.value.refreshNotice)
            assertEquals(3, source.loadCount)
        } finally {
            Dispatchers.resetMain()
        }
    }
}

private fun marketplaceListing() = MarketplaceListingSnapshot(
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
