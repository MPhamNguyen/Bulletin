package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingPage
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSnapshot
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSource
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageCursor
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageRequest
import com.jdrms.bulletin.domain.marketplace.application.SearchMarketplace
import com.jdrms.bulletin.domain.marketplace.application.ToggleSaveMarketplaceItem
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
    fun refreshIncludesListingUploadedAfterViewModelWasCreated() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = InMemoryMarketplaceRepository(initialListings = emptyList())
            val source = MutableListingSource()
            val viewModel = MarketplaceViewModel(
                searchMarketplace = SearchMarketplace(repository, source),
                toggleSaveItem = ToggleSaveMarketplaceItem(repository),
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
                toggleSaveItem = ToggleSaveMarketplaceItem(repository),
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
                toggleSaveItem = ToggleSaveMarketplaceItem(repository),
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
                toggleSaveItem = ToggleSaveMarketplaceItem(repository),
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
                toggleSaveItem = ToggleSaveMarketplaceItem(repository),
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
                toggleSaveItem = ToggleSaveMarketplaceItem(repository),
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

    override suspend fun toggleSaved(userId: String, itemId: MarketplaceItemId): Result<Boolean> =
        delegate.toggleSaved(userId, itemId)

    override suspend fun getSavedItemIds(userId: String): Set<MarketplaceItemId> =
        delegate.getSavedItemIds(userId)
}
