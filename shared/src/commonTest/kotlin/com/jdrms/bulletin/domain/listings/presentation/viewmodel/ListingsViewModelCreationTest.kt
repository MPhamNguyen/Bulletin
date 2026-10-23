package com.jdrms.bulletin.domain.listings.presentation.viewmodel

import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.application.CreateListing
import com.jdrms.bulletin.domain.listings.application.DeleteListing
import com.jdrms.bulletin.domain.listings.application.GetSellerListings
import com.jdrms.bulletin.domain.listings.application.ListingSeller
import com.jdrms.bulletin.domain.listings.application.ManageListing
import com.jdrms.bulletin.domain.listings.application.MarkListingSold
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingCondition
import com.jdrms.bulletin.domain.listings.domain.service.ListingValidationPolicy
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingsRepository
import com.jdrms.bulletin.domain.listings.presentation.ListingsViewModel
import com.jdrms.bulletin.domain.listings.presentation.ListingsViewModelSessionDependencies
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
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ListingsViewModelCreationTest {
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
            markListingSold = MarkListingSold(repository, policy),
            deleteListing = DeleteListing(repository, policy),
            getSellerListings = GetSellerListings(repository),
            session = ListingsViewModelSessionDependencies(sellerProvider, signal)
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
}
