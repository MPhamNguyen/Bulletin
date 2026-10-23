package com.jdrms.bulletin.domain.listings.presentation.viewmodel

import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.application.CreateListing
import com.jdrms.bulletin.domain.listings.application.DeleteListing
import com.jdrms.bulletin.domain.listings.application.GetSellerListings
import com.jdrms.bulletin.domain.listings.application.ListingSeller
import com.jdrms.bulletin.domain.listings.application.ManageListing
import com.jdrms.bulletin.domain.listings.application.MarkListingSold
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.domain.service.ListingValidationPolicy
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingsRepository
import com.jdrms.bulletin.domain.listings.presentation.ListingsViewModel
import com.jdrms.bulletin.domain.listings.presentation.ListingsViewModelSessionDependencies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
class ListingsViewModelDeletionTest {
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
    fun testDeleteRequiresConfirmationAndOwnerCanConfirm() = runTest {
        val listing = testListing(sellerId)
        repository.createListing(listing)
        advanceUntilIdle()

        viewModel.requestDeleteListing(listing)
        assertEquals(listing, viewModel.uiState.value.pendingDeletion)
        assertEquals(listing, repository.getListing(listing.id))

        viewModel.cancelDeleteListing()
        assertNull(viewModel.uiState.value.pendingDeletion)
        assertEquals(listing, repository.getListing(listing.id))

        viewModel.requestDeleteListing(listing)
        viewModel.confirmDeleteListing()
        runCurrent()

        assertNull(viewModel.uiState.value.pendingDeletion)
        assertNull(repository.getListing(listing.id))
        assertEquals("Listing deleted successfully!", viewModel.uiState.value.successMessage)
    }

    @Test
    fun testDeleteRejectsNonOwnerAndKeepsListing() = runTest {
        val listing = testListing(SellerId("other_seller"))
        repository.createListing(listing)
        advanceUntilIdle()

        viewModel.requestDeleteListing(listing)
        viewModel.confirmDeleteListing()
        advanceUntilIdle()

        assertEquals(listing, repository.getListing(listing.id))
        assertEquals("Only the owner can delete this listing.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun testDeleteRequiresAuthenticatedSeller() = runTest {
        val listing = testListing(sellerId)
        repository.createListing(listing)
        sellerProvider.currentSeller = Result.Error(IllegalStateException("session expired"))
        advanceUntilIdle()

        viewModel.requestDeleteListing(listing)
        viewModel.confirmDeleteListing()
        advanceUntilIdle()

        assertEquals(listing, repository.getListing(listing.id))
        assertEquals("An Error has Occured, Please Try Again Later", viewModel.uiState.value.errorMessage)
        assertTrue(!viewModel.uiState.value.isDeleting)
        assertEquals(listing, viewModel.uiState.value.pendingDeletion)
    }

    @Test
    fun testDeleteFailureKeepsConfirmationStateAndShowsError() = runTest {
        val countingRepository = CountingListingsRepository(repository).apply {
            deleteFailure = IllegalStateException("database unavailable")
        }
        val failingViewModel = ListingsViewModel(
            createListing = CreateListing(countingRepository),
            manageListing = ManageListing(countingRepository),
            markListingSold = MarkListingSold(countingRepository),
            deleteListing = DeleteListing(countingRepository),
            getSellerListings = GetSellerListings(countingRepository),
            session = ListingsViewModelSessionDependencies(sellerProvider)
        )
        val listing = testListing(sellerId)
        repository.createListing(listing)
        advanceUntilIdle()

        failingViewModel.requestDeleteListing(listing)
        failingViewModel.confirmDeleteListing()
        advanceUntilIdle()

        assertEquals(1, countingRepository.deleteCount)
        assertEquals(listing, failingViewModel.uiState.value.pendingDeletion)
        assertTrue(!failingViewModel.uiState.value.isDeleting)
        assertEquals("An Error has Occured, Please Try Again Later", failingViewModel.uiState.value.errorMessage)
    }

    @Test
    fun testDeleteLookupFailureKeepsConfirmationStateAndResetsBusyState() = runTest {
        val countingRepository = CountingListingsRepository(repository).apply {
            getListingFailure = IllegalStateException("database unavailable")
        }
        val failingViewModel = ListingsViewModel(
            createListing = CreateListing(countingRepository),
            manageListing = ManageListing(countingRepository),
            markListingSold = MarkListingSold(countingRepository),
            deleteListing = DeleteListing(countingRepository),
            getSellerListings = GetSellerListings(countingRepository),
            session = ListingsViewModelSessionDependencies(sellerProvider)
        )
        val listing = testListing(sellerId)
        repository.createListing(listing)
        advanceUntilIdle()

        failingViewModel.requestDeleteListing(listing)
        failingViewModel.confirmDeleteListing()
        advanceUntilIdle()

        assertEquals(listing, failingViewModel.uiState.value.pendingDeletion)
        assertFalse(failingViewModel.uiState.value.isDeleting)
        assertEquals("An Error has Occured, Please Try Again Later", failingViewModel.uiState.value.errorMessage)
    }

    @Test
    fun testRepeatedDeleteConfirmationOnlyCallsRepositoryOnce() = runTest {
        val countingRepository = CountingListingsRepository(repository)
        val protectedViewModel = ListingsViewModel(
            createListing = CreateListing(countingRepository),
            manageListing = ManageListing(countingRepository),
            markListingSold = MarkListingSold(countingRepository),
            deleteListing = DeleteListing(countingRepository),
            getSellerListings = GetSellerListings(countingRepository),
            session = ListingsViewModelSessionDependencies(sellerProvider)
        )
        val listing = testListing(sellerId)
        repository.createListing(listing)
        advanceUntilIdle()

        protectedViewModel.requestDeleteListing(listing)
        protectedViewModel.confirmDeleteListing()
        protectedViewModel.confirmDeleteListing()
        advanceUntilIdle()

        assertEquals(1, countingRepository.deleteCount)
    }

    @Test
    fun testSuccessfulDeleteEmitsListingRefreshSignal() = runTest {
        val signal = RefreshSignal()
        val signaledViewModel = ListingsViewModel(
            createListing = CreateListing(repository),
            manageListing = ManageListing(repository),
            markListingSold = MarkListingSold(repository),
            deleteListing = DeleteListing(repository),
            getSellerListings = GetSellerListings(repository),
            session = ListingsViewModelSessionDependencies(sellerProvider, signal)
        )
        var signalCount = 0
        val collector = launch { signal.events.collect { signalCount++ } }
        val listing = testListing(sellerId)
        repository.createListing(listing)
        advanceUntilIdle()

        signaledViewModel.requestDeleteListing(listing)
        signaledViewModel.confirmDeleteListing()
        runCurrent()

        assertEquals(1, signalCount)
        collector.cancel()
    }

    @Test
    fun testDeleteListingEmitsListingChangedSignal() = runTest {
        val signal = RefreshSignal()
        var emittedCount = 0
        val job = launch {
            signal.events.collect { emittedCount++ }
        }
        val listing = testListing(sellerId)
        repository.createListing(listing)
        advanceUntilIdle()

        val policy = ListingValidationPolicy()
        val customViewModel = ListingsViewModel(
            createListing = CreateListing(repository, policy),
            manageListing = ManageListing(repository, policy),
            markListingSold = MarkListingSold(repository, policy),
            deleteListing = DeleteListing(repository, policy),
            getSellerListings = GetSellerListings(repository),
            session = ListingsViewModelSessionDependencies(sellerProvider, signal)
        )
        advanceUntilIdle()

        customViewModel.requestDeleteListing(listing)
        customViewModel.confirmDeleteListing()
        advanceUntilIdle()

        assertEquals(1, emittedCount)
        job.cancel()
    }
}
