package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.application.CreateListing
import com.jdrms.bulletin.domain.listings.application.CurrentListingSellerProvider
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
import kotlinx.coroutines.test.advanceTimeBy
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
class ListingsViewModelTest {

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
            getSellerListings = GetSellerListings(repository),
            currentSellerProvider = sellerProvider
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
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
        assertEquals("40", state.editPrice)
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
        assertEquals("", state.editPrice)
    }

    @Test
    fun testEditDraftFieldMutations() = runTest {
        viewModel.onEditTitleChanged("Updated Title")
        viewModel.onEditDescriptionChanged("Updated Description")
        viewModel.onEditPriceChanged("55.50")
        viewModel.onEditCategorySelected(ListingCategory.CLOTHING)
        viewModel.onEditConditionSelected(ListingCondition.NEW)

        val state = viewModel.uiState.value
        assertEquals("Updated Title", state.editTitle)
        assertEquals("Updated Description", state.editDescription)
        assertEquals("55.50", state.editPrice)
        assertEquals(ListingCategory.CLOTHING, state.editCategory)
        assertEquals(ListingCondition.NEW, state.editCondition)
    }

    @Test
    fun testSaveListingChangesValidatesPriceAndTitle() = runTest {
        val listing = testListing(sellerId)
        repository.createListing(listing)
        advanceUntilIdle()

        viewModel.startEditing(listing)
        advanceUntilIdle()

        viewModel.onEditPriceChanged("-5")
        viewModel.saveListingChanges()
        assertEquals("Please enter a valid price ($ >= 0)", viewModel.uiState.value.errorMessage)

        viewModel.onEditPriceChanged("abc")
        viewModel.saveListingChanges()
        assertEquals("Please enter a valid price ($ >= 0)", viewModel.uiState.value.errorMessage)

        viewModel.onEditPriceChanged("25.0")
        viewModel.onEditTitleChanged("No")
        viewModel.saveListingChanges()
        assertEquals("Title must be at least 3 characters", viewModel.uiState.value.errorMessage)

        viewModel.onEditTitleChanged("Valid Title")
        viewModel.onEditDescriptionChanged("   ")
        viewModel.saveListingChanges()
        assertEquals("Description cannot be empty", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun testSaveListingChangesSucceedsAndUpdatesRepository() = runTest {
        val listing = testListing(sellerId)
        repository.createListing(listing)
        advanceUntilIdle()

        viewModel.startEditing(listing)
        advanceUntilIdle()

        viewModel.onEditTitleChanged("New Desk Lamp")
        viewModel.onEditDescriptionChanged("Brand new LED desk lamp with USB charging")
        viewModel.onEditPriceChanged("35")
        viewModel.onEditCategorySelected(ListingCategory.ELECTRONICS)

        viewModel.saveListingChanges()
        runCurrent()

        val state = viewModel.uiState.value
        assertNull(state.editingListing)
        assertEquals("Listing updated successfully!", state.successMessage)
        assertNull(state.errorMessage)

        val updatedListing = state.myListings.firstOrNull { it.id == listing.id }
        assertNotNull(updatedListing)
        assertEquals("New Desk Lamp", updatedListing.title)
        assertEquals("Brand new LED desk lamp with USB charging", updatedListing.description)
        assertEquals(35.0, updatedListing.price.amount)
        assertEquals(ListingCategory.ELECTRONICS, updatedListing.category)

        advanceTimeBy(ListingsViewModel.FLASH_NOTIFICATION_DURATION_MILLIS)
        runCurrent()
        assertNull(viewModel.uiState.value.successMessage)
    }

    @Test
    fun testFlashNotificationAutoDismissesAfterDelay() = runTest {
        val listing = testListing(sellerId)
        repository.createListing(listing)
        advanceUntilIdle()

        viewModel.startEditing(listing)
        advanceUntilIdle()

        viewModel.saveListingChanges()
        runCurrent()

        assertEquals("Listing updated successfully!", viewModel.uiState.value.successMessage)

        advanceTimeBy(ListingsViewModel.FLASH_NOTIFICATION_DURATION_MILLIS - 1)
        runCurrent()
        assertEquals("Listing updated successfully!", viewModel.uiState.value.successMessage)

        advanceTimeBy(1)
        runCurrent()
        assertNull(viewModel.uiState.value.successMessage)
    }

    @Test
    fun testClearMessagesCancelsFlashNotificationImmediately() = runTest {
        val listing = testListing(sellerId)
        repository.createListing(listing)
        advanceUntilIdle()

        viewModel.startEditing(listing)
        advanceUntilIdle()

        viewModel.saveListingChanges()
        runCurrent()

        assertEquals("Listing updated successfully!", viewModel.uiState.value.successMessage)

        viewModel.clearMessages()
        assertNull(viewModel.uiState.value.successMessage)

        advanceTimeBy(ListingsViewModel.FLASH_NOTIFICATION_DURATION_MILLIS)
        runCurrent()
        assertNull(viewModel.uiState.value.successMessage)
    }

    @Test
    fun testSubmitNewListingShowsFlashNotificationAndAutoDismisses() = runTest {
        viewModel.onTitleChanged("Brand New Textbook")
        viewModel.onDescriptionChanged("Used for CS 101, excellent condition")
        viewModel.onPriceChanged("45.00")
        viewModel.onCategorySelected(ListingCategory.TEXTBOOKS)
        viewModel.onConditionSelected(ListingCondition.LIKE_NEW)

        viewModel.submitNewListing()
        runCurrent()

        assertEquals("Listing posted successfully!", viewModel.uiState.value.successMessage)

        advanceTimeBy(ListingsViewModel.FLASH_NOTIFICATION_DURATION_MILLIS)
        runCurrent()
        assertNull(viewModel.uiState.value.successMessage)
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

        viewModel.deleteListing(firstListing.id)
        advanceUntilIdle()

        assertEquals(listOf("Study Chair"), viewModel.uiState.value.myListings.map { it.title })
    }

    @Test
    fun deletingAnUnknownListingLeavesTheLoadedMyListingsStateUnchanged() = runTest {
        val listing = testListing(sellerId).copy(id = ListingId("listing-1"), title = "Desk Lamp")
        repository.createListing(listing)
        viewModel.loadMyListings(ListingSeller(sellerId, sellerName))
        advanceUntilIdle()

        viewModel.deleteListing(ListingId("missing-listing"))
        advanceUntilIdle()

        assertEquals(listOf("Desk Lamp"), viewModel.uiState.value.myListings.map { it.title })
    }

    @Test
    fun testSubmitListingEmitsListingChangedSignal() = runTest {
        val signal = RefreshSignal()
        var emittedCount = 0
        val job = launch {
            signal.events.collect { emittedCount++ }
        }
        val policy = ListingValidationPolicy()
        val customViewModel = ListingsViewModel(
            createListing = CreateListing(repository, policy),
            manageListing = ManageListing(repository, policy),
            getSellerListings = GetSellerListings(repository),
            currentSellerProvider = sellerProvider,
            listingChangedSignal = signal
        )
        advanceUntilIdle()

        customViewModel.onTitleChanged("Signal Test Item")
        customViewModel.onDescriptionChanged("Valid signal description")
        customViewModel.onPriceChanged("15.00")
        customViewModel.onCategorySelected(ListingCategory.OTHER)
        customViewModel.onConditionSelected(ListingCondition.GOOD)

        customViewModel.submitNewListing()
        advanceUntilIdle()

        assertEquals(1, emittedCount)
        job.cancel()
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
            getSellerListings = GetSellerListings(repository),
            currentSellerProvider = sellerProvider,
            listingChangedSignal = signal
        )
        advanceUntilIdle()

        customViewModel.deleteListing(listing.id)
        advanceUntilIdle()

        assertEquals(1, emittedCount)
        job.cancel()
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
}

private class CountingListingsRepository(
    private val delegate: ListingsRepository
) : ListingsRepository {
    var updateCount = 0

    override suspend fun createListing(listing: Listing): Result<Listing> = delegate.createListing(listing)

    override suspend fun updateListing(listing: Listing): Result<Listing> {
        updateCount += 1
        delay(10)
        return delegate.updateListing(listing)
    }

    override suspend fun getListing(id: ListingId): Listing? = delegate.getListing(id)

    override suspend fun deleteListing(id: ListingId): Result<Unit> = delegate.deleteListing(id)

    override suspend fun getSellerListings(sellerId: SellerId): List<Listing> = delegate.getSellerListings(sellerId)

    override suspend fun getAllListings(): List<Listing> = delegate.getAllListings()
}
