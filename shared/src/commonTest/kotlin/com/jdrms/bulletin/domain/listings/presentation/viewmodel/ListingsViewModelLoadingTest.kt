package com.jdrms.bulletin.domain.listings.presentation.viewmodel

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.application.CreateListing
import com.jdrms.bulletin.domain.listings.application.DeleteListing
import com.jdrms.bulletin.domain.listings.application.GetSellerListings
import com.jdrms.bulletin.domain.listings.application.ListingSeller
import com.jdrms.bulletin.domain.listings.application.ManageListing
import com.jdrms.bulletin.domain.listings.application.MarkListingSold
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.service.ListingValidationPolicy
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingsRepository
import com.jdrms.bulletin.domain.listings.presentation.ListingsLoadState
import com.jdrms.bulletin.domain.listings.presentation.ListingsViewModel
import com.jdrms.bulletin.domain.listings.presentation.ListingsViewModelSessionDependencies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ListingsViewModelLoadingTest {
    private val testDispatcher = StandardTestDispatcher()
    private val sellerId = TEST_SELLER_ID
    private val sellerName = TEST_SELLER_NAME
    private lateinit var sellerProvider: FakeListingSellerProvider
    private lateinit var repository: InMemoryListingsRepository
    private lateinit var viewModel: ListingsViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sellerProvider = FakeListingSellerProvider(Result.Success(ListingSeller(sellerId, sellerName)))
        repository = InMemoryListingsRepository(initialListings = emptyList())
        val policy = ListingValidationPolicy()
        viewModel = ListingsViewModel(
            createListing = CreateListing(repository, policy),
            manageListing = ManageListing(repository, policy),
            deleteListing = DeleteListing(repository, policy),
            markListingSold = MarkListingSold(repository, policy),
            getSellerListings = GetSellerListings(repository),
            session = ListingsViewModelSessionDependencies(sellerProvider)
        )
    }

    @AfterTest
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun testInitialLoadingAndRefreshFailureRetainPriorContent() = runTest {
        advanceUntilIdle()
        assertEquals(ListingsLoadState.LOADED, viewModel.uiState.value.loadState)
        assertTrue(viewModel.uiState.value.myListings.isEmpty())

        val listing = testListing(sellerId)
        repository.createListing(listing)
        viewModel.loadMyListings(ListingSeller(sellerId, sellerName))
        advanceUntilIdle()
        assertEquals(listOf(listing), viewModel.uiState.value.myListings)

        sellerProvider.currentSeller = Result.Error(IllegalStateException("session expired"))
        viewModel.loadMyListings()
        advanceUntilIdle()

        assertEquals(ListingsLoadState.FAILED, viewModel.uiState.value.loadState)
        assertEquals(listOf(listing), viewModel.uiState.value.myListings)
    }

    @Test
    fun loadingAnEmptySellerRepositoryProducesTheMyListingsEmptyState() = runTest {
        advanceUntilIdle()

        assertEquals(emptyList(), viewModel.uiState.value.myListings)
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun loadingMyListingsThenDeletingOneRefreshesTheRemainingListings() = runTest {
        val firstListing = testListing(sellerId).copy(
            id = ListingId("listing-1"),
            title = "Desk Lamp"
        )
        val secondListing = testListing(sellerId).copy(
            id = ListingId("listing-2"),
            title = "Study Chair"
        )
        repository.createListing(firstListing)
        repository.createListing(secondListing)
        viewModel.loadMyListings(ListingSeller(sellerId, sellerName))
        advanceUntilIdle()

        assertEquals(
            setOf("Desk Lamp", "Study Chair"),
            viewModel.uiState.value.myListings.map { it.title }.toSet()
        )

        viewModel.requestDeleteListing(firstListing)
        viewModel.confirmDeleteListing()
        advanceUntilIdle()

        assertEquals(listOf("Study Chair"), viewModel.uiState.value.myListings.map { it.title })
    }

    @Test
    fun deletingAnUnknownListingLeavesTheLoadedMyListingsStateUnchanged() = runTest {
        val listing = testListing(sellerId).copy(id = ListingId("listing-1"), title = "Desk Lamp")
        repository.createListing(listing)
        viewModel.loadMyListings(ListingSeller(sellerId, sellerName))
        advanceUntilIdle()

        viewModel.requestDeleteListing(listing.copy(id = ListingId("missing-listing")))
        viewModel.confirmDeleteListing()
        advanceUntilIdle()

        assertEquals(listOf("Desk Lamp"), viewModel.uiState.value.myListings.map { it.title })
    }
}
