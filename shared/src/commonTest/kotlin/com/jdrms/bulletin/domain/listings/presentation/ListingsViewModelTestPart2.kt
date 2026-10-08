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
import com.jdrms.bulletin.domain.listings.domain.service.ListingValidationPolicy
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class ListingsViewModelTestPart2 {

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
    fun testEditDraftFieldMutations() = runTest {
        viewModel.onEditTitleChanged("Updated Title")
        viewModel.onEditDescriptionChanged("Updated Description")
        viewModel.onEditPriceChanged("55.50")
        viewModel.onEditCategorySelected(ListingCategory.CLOTHING)
        viewModel.onEditConditionSelected(ListingCondition.NEW)

        val state = viewModel.uiState.value
        assertEquals("Updated Title", state.editTitle)
        assertEquals("Updated Description", state.editDescription)
        assertEquals("5550", state.editPriceCents)
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

        viewModel.onEditPriceChanged("")
        viewModel.saveListingChanges()
        assertEquals("Please enter a price", viewModel.uiState.value.errorMessage)

        viewModel.onEditPriceChanged("abc")
        viewModel.saveListingChanges()
        assertEquals("Please enter a price", viewModel.uiState.value.errorMessage)

        viewModel.onEditPriceChanged("40555")
        assertEquals("40555", viewModel.uiState.value.editPriceCents)

        viewModel.onEditPriceChanged("2500")
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
        viewModel.onEditPriceChanged("3500")
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
    fun testSubmitNewListingTreatsEveryPriceDigitAsCents() = runTest {
        viewModel.onTitleChanged("Brand New Textbook")
        viewModel.onDescriptionChanged("Used for CS 101, excellent condition")
        viewModel.onPriceChanged("45555")

        viewModel.submitNewListing()

        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(455.55, repository.getAllListings().single().price.amount)
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
            deleteListing = DeleteListing(repository, policy),
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
            deleteListing = DeleteListing(repository, policy),
            getSellerListings = GetSellerListings(repository),
            currentSellerProvider = sellerProvider,
            listingChangedSignal = signal
        )
        advanceUntilIdle()

        customViewModel.requestDeleteListing(listing)
        customViewModel.confirmDeleteListing()
        advanceUntilIdle()

        assertEquals(1, emittedCount)
        job.cancel()
    }
}
