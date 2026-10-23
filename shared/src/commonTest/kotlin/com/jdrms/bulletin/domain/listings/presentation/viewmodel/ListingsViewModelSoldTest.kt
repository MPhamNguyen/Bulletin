package com.jdrms.bulletin.domain.listings.presentation.viewmodel

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.application.CreateListing
import com.jdrms.bulletin.domain.listings.application.DeleteListing
import com.jdrms.bulletin.domain.listings.application.GetSellerListings
import com.jdrms.bulletin.domain.listings.application.ListingSeller
import com.jdrms.bulletin.domain.listings.application.ManageListing
import com.jdrms.bulletin.domain.listings.application.MarkListingSold
import com.jdrms.bulletin.domain.listings.application.RecentListingBuyer
import com.jdrms.bulletin.domain.listings.application.RecentListingBuyersProvider
import com.jdrms.bulletin.domain.listings.domain.model.ListingStatus
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.domain.service.ListingValidationPolicy
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingsRepository
import com.jdrms.bulletin.domain.listings.presentation.ListingsViewModel
import com.jdrms.bulletin.domain.listings.presentation.ListingsViewModelSessionDependencies
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class ListingsViewModelSoldTest {
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
    fun testMarkSoldRequiresConfirmationAndUpdatesListing() = runTest {
        val listing = testListing(sellerId)
        repository.createListing(listing)
        advanceUntilIdle()

        viewModel.requestMarkListingSold(listing)
        assertEquals(listing, viewModel.uiState.value.pendingSold)

        viewModel.confirmMarkListingSold()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.pendingSold)
        assertEquals(ListingStatus.SOLD, repository.getListing(listing.id)?.status)
    }

    @Test
    fun cancelledSoldBuyerLookupCannotRestoreTheDialog() = runTest {
        val listing = testListing(sellerId)
        val pendingBuyers = CompletableDeferred<List<RecentListingBuyer>>()
        val lookupProvider = RecentListingBuyersProvider { _, _ -> pendingBuyers.await() }
        val lookupViewModel = ListingsViewModel(
            createListing = CreateListing(repository),
            manageListing = ManageListing(repository),
            markListingSold = MarkListingSold(repository),
            recentListingBuyersProvider = lookupProvider,
            deleteListing = DeleteListing(repository),
            getSellerListings = GetSellerListings(repository),
            session = ListingsViewModelSessionDependencies(sellerProvider)
        )

        lookupViewModel.requestMarkListingSold(listing)
        runCurrent()
        lookupViewModel.cancelMarkListingSold()
        pendingBuyers.complete(listOf(RecentListingBuyer("buyer_1", "Buyer One")))
        advanceUntilIdle()

        assertNull(lookupViewModel.uiState.value.pendingSold)
        assertTrue(lookupViewModel.uiState.value.recentSoldBuyers.isEmpty())
    }

    @Test
    fun testMarkSoldRejectsNonOwnerAndKeepsDialogOpenWithError() = runTest {
        val listing = testListing(SellerId("other_seller"))
        repository.createListing(listing)
        advanceUntilIdle()

        viewModel.requestMarkListingSold(listing)
        viewModel.confirmMarkListingSold()
        runCurrent()

        assertEquals(listing, viewModel.uiState.value.pendingSold)
        assertEquals("Only the owner can mark as sold this listing.", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isMarkingSold)
        assertEquals(ListingStatus.AVAILABLE, repository.getListing(listing.id)?.status)
    }

    @Test
    fun testMarkSoldRequiresAuthenticatedSellerAndKeepsDialogOpenWithError() = runTest {
        val listing = testListing(sellerId)
        repository.createListing(listing)
        sellerProvider.currentSeller = Result.Error(IllegalStateException("session expired"))
        advanceUntilIdle()

        viewModel.requestMarkListingSold(listing)
        viewModel.confirmMarkListingSold()
        runCurrent()

        assertEquals(listing, viewModel.uiState.value.pendingSold)
        assertEquals("An Error has Occured, Please Try Again Later", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isMarkingSold)
    }

    @Test
    fun testMarkSoldRejectsAlreadySoldListingAndKeepsDialogOpenWithError() = runTest {
        val listing = testListing(sellerId).markSold(sellerId)
        repository.createListing(listing)
        advanceUntilIdle()

        viewModel.requestMarkListingSold(listing)
        viewModel.confirmMarkListingSold()
        advanceUntilIdle()

        assertEquals(listing, viewModel.uiState.value.pendingSold)
        assertEquals("An Error has Occured, Please Try Again Later", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isMarkingSold)
        assertEquals(ListingStatus.SOLD, repository.getListing(listing.id)?.status)
    }

    @Test
    fun testMarkSoldRepositoryFailureKeepsDialogOpenWithError() = runTest {
        val failingRepository = CountingListingsRepository(repository).apply {
            updateFailure = IllegalStateException("database unavailable")
        }
        val failingViewModel = ListingsViewModel(
            createListing = CreateListing(failingRepository),
            manageListing = ManageListing(failingRepository),
            markListingSold = MarkListingSold(failingRepository),
            deleteListing = DeleteListing(failingRepository),
            getSellerListings = GetSellerListings(failingRepository),
            session = ListingsViewModelSessionDependencies(sellerProvider)
        )
        val listing = testListing(sellerId)
        repository.createListing(listing)
        advanceUntilIdle()

        failingViewModel.requestMarkListingSold(listing)
        failingViewModel.confirmMarkListingSold()
        advanceUntilIdle()

        assertEquals(listing, failingViewModel.uiState.value.pendingSold)
        assertEquals("An Error has Occured, Please Try Again Later", failingViewModel.uiState.value.errorMessage)
        assertFalse(failingViewModel.uiState.value.isMarkingSold)
        assertEquals(ListingStatus.AVAILABLE, repository.getListing(listing.id)?.status)
    }
}
