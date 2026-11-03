package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.application.CreateListing
import com.jdrms.bulletin.domain.listings.application.CurrentListingSellerProvider
import com.jdrms.bulletin.domain.listings.application.DeleteListing
import com.jdrms.bulletin.domain.listings.application.GetSellerListings
import com.jdrms.bulletin.domain.listings.application.ListingSeller
import com.jdrms.bulletin.domain.listings.application.ManageListing
import com.jdrms.bulletin.domain.listings.application.ManageListingImages
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.ListingImageUpload
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.domain.repository.ListingImagesRepository
import com.jdrms.bulletin.domain.listings.domain.service.ListingPhotoPolicy
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingImagesRepository
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ListingCreationActionsTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = InMemoryListingsRepository(initialListings = emptyList())
    private val signal = RefreshSignal()
    private val storedImages = InMemoryListingImagesRepository()
    private var uploadedPhotos: List<ListingImageUpload> = emptyList()
    private val images = object : ListingImagesRepository by storedImages {
        override suspend fun synchronize(
            listingId: ListingId,
            sellerId: SellerId,
            retainedImageUrls: List<String>,
            newImages: List<ListingImageUpload>
        ): Result<List<String>> {
            uploadedPhotos = newImages
            return storedImages.synchronize(listingId, sellerId, retainedImageUrls, newImages)
        }
    }
    private lateinit var viewModel: ListingsViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val sellerProvider = object : CurrentListingSellerProvider {
            override suspend fun getCurrentSeller() = Result.Success(ListingSeller(SellerId("seller"), "Seller"))
        }
        viewModel = ListingsViewModel(
            createListing = CreateListing(repository),
            manageListing = ManageListing(repository),
            deleteListing = DeleteListing(repository),
            getSellerListings = GetSellerListings(repository),
            session = ListingsViewModelSessionDependencies(
                sellerProvider, signal, ManageListingImages(images)
            )
        )
        viewModel.onTitleChanged("Campus textbook")
        viewModel.onDescriptionChanged("Meet on campus")
        viewModel.onPriceChanged("45555")
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun submissionWithoutAPhotoDoesNotCreateAListing() = runTest {
        viewModel.submitNewListing()
        advanceUntilIdle()

        assertTrue(repository.getAllListings().isEmpty())
        assertEquals(ListingPhotoPolicy.REQUIRED_PHOTO_MESSAGE, viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertEquals("Campus textbook", viewModel.uiState.value.newTitle)
    }

    @Test
    fun removingTheLastPhotoAlsoBlocksDirectSubmission() = runTest {
        addPhoto()
        viewModel.removeNewImage(0)
        viewModel.submitNewListing()
        advanceUntilIdle()

        assertTrue(repository.getAllListings().isEmpty())
        assertEquals(ListingPhotoPolicy.REQUIRED_PHOTO_MESSAGE, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun submissionWithOnePhotoUploadsItAndPersistsPriceInCents() = runTest {
        addPhoto()
        viewModel.submitNewListing()
        advanceUntilIdle()

        val listing = repository.getAllListings().single()
        assertEquals(1, uploadedPhotos.size)
        assertEquals(455.55, listing.price.amount)
        assertNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.newImages.isEmpty())
    }

    @Test
    fun successfulSubmissionShowsFlashNotificationAndAutoDismisses() = runTest {
        addPhoto()
        viewModel.submitNewListing()
        runCurrent()
        assertEquals("Listing posted successfully!", viewModel.uiState.value.successMessage)

        advanceTimeBy(ListingsViewModel.FLASH_NOTIFICATION_DURATION_MILLIS)
        runCurrent()
        assertNull(viewModel.uiState.value.successMessage)
    }

    @Test
    fun successfulSubmissionEmitsListingChangedSignal() = runTest {
        var emittedCount = 0
        val job = launch { signal.events.collect { emittedCount++ } }
        runCurrent()
        addPhoto()
        viewModel.submitNewListing()
        advanceUntilIdle()

        assertEquals(1, emittedCount)
        job.cancel()
    }

    private fun addPhoto() {
        viewModel.addNewImages(listOf(ListingImageUpload(byteArrayOf(1), "image/jpeg")))
    }
}
