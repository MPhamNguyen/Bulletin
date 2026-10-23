package com.jdrms.bulletin.domain.listings.presentation.viewmodel

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.application.CreateListing
import com.jdrms.bulletin.domain.listings.application.DeleteListing
import com.jdrms.bulletin.domain.listings.application.GetSellerListings
import com.jdrms.bulletin.domain.listings.application.ListingSeller
import com.jdrms.bulletin.domain.listings.application.ManageListing
import com.jdrms.bulletin.domain.listings.application.MarkListingSold
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingCondition
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.domain.service.ListingValidationPolicy
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingsRepository
import com.jdrms.bulletin.domain.listings.presentation.ListingsViewModel
import com.jdrms.bulletin.domain.listings.presentation.ListingsViewModelSessionDependencies
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

@OptIn(ExperimentalCoroutinesApi::class)
class ListingsViewModelEditingTest {
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
    fun testImmediateDuplicateSavesInvokeRepositoryOnce() = runTest {
        val baseRepository = InMemoryListingsRepository(initialListings = emptyList())
        val countingRepository = CountingListingsRepository(baseRepository)
        val listing = testListing(sellerId)
        baseRepository.createListing(listing)
        val duplicateSaveViewModel = ListingsViewModel(
            createListing = CreateListing(countingRepository),
            manageListing = ManageListing(countingRepository),
            markListingSold = MarkListingSold(countingRepository),
            deleteListing = DeleteListing(countingRepository),
            getSellerListings = GetSellerListings(countingRepository),
            session = ListingsViewModelSessionDependencies(sellerProvider)
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
        assertEquals("", state.editPriceCents)
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
}
