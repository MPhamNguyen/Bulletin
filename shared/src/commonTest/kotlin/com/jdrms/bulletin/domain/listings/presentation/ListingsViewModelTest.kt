package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.application.CreateListing
import com.jdrms.bulletin.domain.listings.application.CurrentListingSellerProvider
import com.jdrms.bulletin.domain.listings.application.DeleteListing
import com.jdrms.bulletin.domain.listings.application.GetSellerListings
import com.jdrms.bulletin.domain.listings.application.ListingSeller
import com.jdrms.bulletin.domain.listings.application.ManageListing
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingCondition
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.ListingPrice
import com.jdrms.bulletin.domain.listings.domain.model.ListingStatus
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.domain.repository.ListingsRepository
import com.jdrms.bulletin.domain.listings.domain.service.ListingValidationPolicy
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ListingsViewModelTestPart1 {

    private val testDispatcher = StandardTestDispatcher()
    private val sellerId = SellerId("seller_123")
    private val sellerName = "Dominic Student"
    private lateinit var sellerProvider: FakeListingSellerProvider
    private lateinit var repository: InMemoryListingsRepository
    private lateinit var viewModel: ListingsViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sellerProvider = FakeListingSellerProvider(
            Result.Success(ListingSeller(sellerId, sellerName))
        )
        repository = InMemoryListingsRepository(initialListings = emptyList())
        val policy = ListingValidationPolicy()
        viewModel = ListingsViewModel(
            createListing = CreateListing(repository, policy),
            manageListing = ManageListing(repository, policy),
            deleteListing = DeleteListing(repository, policy),
            getSellerListings = GetSellerListings(repository),
            currentSellerProvider = sellerProvider
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun testListing(ownerId: SellerId): Listing {
        return Listing(
            id = ListingId("list_test_vm"),
            sellerId = ownerId,
            sellerName = sellerName,
            title = "Tactile Keyboard",
            description = "Quiet tactile switches for studying",
            price = ListingPrice(40.0),
            category = ListingCategory.ELECTRONICS,
            condition = ListingCondition.GOOD,
            status = ListingStatus.AVAILABLE
        )
    }

    private class FakeListingSellerProvider(
        var currentSeller: Result<ListingSeller>
    ) : CurrentListingSellerProvider {
        override suspend fun getCurrentSeller(): Result<ListingSeller> = currentSeller
    }

    @Test
    fun testStartEditingPopulatesStateForOwner() = runTest {
        val listing = testListing(sellerId)
        repository.createListing(listing)
        advanceUntilIdle()

        viewModel.startEditing(listing)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listing, state.editingListing)
        assertEquals(listing.title, state.editTitle)
        assertEquals(listing.description, state.editDescription)
        assertEquals("4000", state.editPriceCents)
        assertEquals(listing.category, state.editCategory)
        assertEquals(listing.condition, state.editCondition)
        assertNull(state.errorMessage)
    }

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
    fun testImmediateDuplicateSavesInvokeRepositoryOnce() = runTest {
        val baseRepository = InMemoryListingsRepository(initialListings = emptyList())
        val countingRepository = CountingListingsRepository(baseRepository)
        val listing = testListing(sellerId)
        baseRepository.createListing(listing)
        val duplicateSaveViewModel = ListingsViewModel(
            createListing = CreateListing(countingRepository),
            manageListing = ManageListing(countingRepository),
            deleteListing = DeleteListing(countingRepository),
            getSellerListings = GetSellerListings(countingRepository),
            currentSellerProvider = sellerProvider
        )
        advanceUntilIdle()
        duplicateSaveViewModel.startEditing(listing)
        advanceUntilIdle()
        duplicateSaveViewModel.onEditTitleChanged("Updated once")

        duplicateSaveViewModel.saveListingChanges()
        duplicateSaveViewModel.saveListingChanges()
        advanceUntilIdle()

        assertEquals(1, countingRepository.updateCount)
    }

    @Test
    fun testStartEditingRejectsNonOwner() = runTest {
        val listing = testListing(SellerId("other_seller"))
        repository.createListing(listing)
        advanceUntilIdle()

        viewModel.startEditing(listing)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.editingListing)
        assertEquals("Only the owner can edit this listing.", state.errorMessage)
    }

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
            deleteListing = DeleteListing(countingRepository),
            getSellerListings = GetSellerListings(countingRepository),
            currentSellerProvider = sellerProvider
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
            deleteListing = DeleteListing(countingRepository),
            getSellerListings = GetSellerListings(countingRepository),
            currentSellerProvider = sellerProvider
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
            deleteListing = DeleteListing(countingRepository),
            getSellerListings = GetSellerListings(countingRepository),
            currentSellerProvider = sellerProvider
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
            deleteListing = DeleteListing(repository),
            getSellerListings = GetSellerListings(repository),
            currentSellerProvider = sellerProvider,
            listingChangedSignal = signal
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
    fun testCancelEditingClearsDraft() = runTest {
        val listing = testListing(sellerId)
        repository.createListing(listing)
        advanceUntilIdle()

        viewModel.startEditing(listing)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.editingListing)

        viewModel.cancelEditing()

        val state = viewModel.uiState.value
        assertNull(state.editingListing)
        assertEquals("", state.editTitle)
        assertEquals("", state.editDescription)
        assertEquals("", state.editPriceCents)
    }
}

private class CountingListingsRepository(
    private val delegate: ListingsRepository
) : ListingsRepository {
    var updateCount = 0
    var deleteCount = 0
    var deleteFailure: Throwable? = null
    var getListingFailure: Throwable? = null

    override suspend fun createListing(listing: Listing): Result<Listing> = delegate.createListing(listing)

    override suspend fun updateListing(listing: Listing): Result<Listing> {
        updateCount += 1
        delay(10)
        return delegate.updateListing(listing)
    }

    override suspend fun getListing(id: ListingId): Listing? {
        getListingFailure?.let { throw it }
        return delegate.getListing(id)
    }

    override suspend fun deleteListing(id: ListingId, sellerId: SellerId): Result<Unit> {
        deleteCount += 1
        return deleteFailure?.let { Result.Error(it) } ?: delegate.deleteListing(id, sellerId)
    }

    override suspend fun getSellerListings(sellerId: SellerId): List<Listing> = delegate.getSellerListings(sellerId)

    override suspend fun getAllListings(): List<Listing> = delegate.getAllListings()
}
