package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSnapshot
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSource
import com.jdrms.bulletin.domain.marketplace.application.SearchMarketplace
import com.jdrms.bulletin.domain.marketplace.application.ToggleSaveMarketplaceItem
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

class MarketplaceViewModelTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun refreshIncludesListingUploadedAfterViewModelWasCreated() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = InMemoryMarketplaceRepository(initialItems = emptyList())
            val source = MutableListingSource()
            val viewModel = MarketplaceViewModel(
                searchMarketplace = SearchMarketplace(repository, source),
                toggleSaveItem = ToggleSaveMarketplaceItem(repository)
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
            assertEquals(2, source.loadCount)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun loadFailureStopsLoadingAndExposesRetryableError() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = InMemoryMarketplaceRepository(initialItems = emptyList())
            val viewModel = MarketplaceViewModel(
                searchMarketplace = SearchMarketplace(repository, FailingListingSource),
                toggleSaveItem = ToggleSaveMarketplaceItem(repository)
            )

            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertNotNull(viewModel.uiState.value.errorMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

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
}

private object FailingListingSource : MarketplaceListingSource {
    override suspend fun getAvailableListings(): List<MarketplaceListingSnapshot> {
        error("Database unavailable")
    }
}

private class MutableListingSource : MarketplaceListingSource {
    val listings = mutableListOf<MarketplaceListingSnapshot>()
    var loadCount = 0

    override suspend fun getAvailableListings(): List<MarketplaceListingSnapshot> {
        loadCount += 1
        return listings.toList()
    }
}
