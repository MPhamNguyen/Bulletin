package com.jdrms.bulletin.domain.listings.presentation

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
import com.jdrms.bulletin.domain.listings.domain.service.ListingValidationPolicy
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
        assertEquals(listing.images, state.editImages)
        assertNull(state.errorMessage)
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
        assertTrue(state.editImages.isEmpty())
    }

    @Test
    fun testEditDraftFieldMutations() = runTest {
        viewModel.onEditTitleChanged("Updated Title")
        viewModel.onEditDescriptionChanged("Updated Description")
        viewModel.onEditPriceChanged("55.50")
        viewModel.onEditCategorySelected(ListingCategory.CLOTHING)
        viewModel.onEditConditionSelected(ListingCondition.NEW)
        viewModel.onAddEditImage("https://example.com/photo.png")

        val state = viewModel.uiState.value
        assertEquals("Updated Title", state.editTitle)
        assertEquals("Updated Description", state.editDescription)
        assertEquals("55.50", state.editPrice)
        assertEquals(ListingCategory.CLOTHING, state.editCategory)
        assertEquals(ListingCondition.NEW, state.editCondition)
        assertEquals(listOf("https://example.com/photo.png"), state.editImages)

        viewModel.onRemoveEditImage(0)
        assertTrue(viewModel.uiState.value.editImages.isEmpty())

        viewModel.onAddEditImage("https://example.com/photo1.png")
        viewModel.onAddEditImage("https://example.com/photo2.png")
        assertEquals(2, viewModel.uiState.value.editImages.size)

        viewModel.onClearEditImages()
        assertTrue(viewModel.uiState.value.editImages.isEmpty())
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
        viewModel.onAddEditImage("https://example.com/lamp1.jpg")
        viewModel.onAddEditImage("https://example.com/lamp2.jpg")

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
        assertEquals(2, updatedListing.images.size)
        assertEquals("https://example.com/lamp1.jpg", updatedListing.images[0])

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
