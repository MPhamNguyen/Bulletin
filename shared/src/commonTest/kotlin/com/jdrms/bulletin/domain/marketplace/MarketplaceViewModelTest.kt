package com.jdrms.bulletin.domain.marketplace

import com.jdrms.bulletin.domain.marketplace.application.SearchMarketplace
import com.jdrms.bulletin.domain.marketplace.application.ToggleSaveMarketplaceItem
import com.jdrms.bulletin.domain.marketplace.application.ViewMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryMarketplaceRepository
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MarketplaceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: InMemoryMarketplaceRepository
    private lateinit var searchMarketplace: SearchMarketplace
    private lateinit var toggleSaveItem: ToggleSaveMarketplaceItem
    private lateinit var viewMarketplaceListing: ViewMarketplaceListing
    private lateinit var viewModel: MarketplaceViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = InMemoryMarketplaceRepository()
        searchMarketplace = SearchMarketplace(repository)
        toggleSaveItem = ToggleSaveMarketplaceItem(repository)
        viewMarketplaceListing = ViewMarketplaceListing(repository)
        viewModel = MarketplaceViewModel(
            searchMarketplace = searchMarketplace,
            toggleSaveItem = toggleSaveItem,
            viewMarketplaceListing = viewMarketplaceListing
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testOnListingClickedOpensSheetAndLoadsDetail() = runTest {
        viewModel.onListingClicked("mkt_1")

        // Sheet is immediately opened with loading
        assertTrue(viewModel.uiState.value.isDetailSheetOpen)
        assertEquals("mkt_1", viewModel.uiState.value.selectedListingId)

        // Advance coroutines
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isDetailLoading)
        assertNull(viewModel.uiState.value.detailErrorMessage)
        val selected = viewModel.uiState.value.selectedListing
        assertNotNull(selected)
        assertEquals("Calculus: Early Transcendentals (8th Ed)", selected.title)
        assertEquals("Dominic Alfonso", selected.sellerName)
        assertEquals(4.8, selected.sellerReputationScore)
    }

    @Test
    fun testDismissListingDetailClosesSheet() = runTest {
        viewModel.onListingClicked("mkt_1")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isDetailSheetOpen)

        viewModel.dismissListingDetail()
        assertFalse(viewModel.uiState.value.isDetailSheetOpen)
        assertNull(viewModel.uiState.value.selectedListing)
        assertNull(viewModel.uiState.value.selectedListingId)
    }

    @Test
    fun testOnListingClickedHandlesNotFound() = runTest {
        viewModel.onListingClicked("missing_item_999")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isDetailSheetOpen)
        assertFalse(viewModel.uiState.value.isDetailLoading)
        assertNull(viewModel.uiState.value.selectedListing)
        assertNotNull(viewModel.uiState.value.detailErrorMessage)
    }
}
