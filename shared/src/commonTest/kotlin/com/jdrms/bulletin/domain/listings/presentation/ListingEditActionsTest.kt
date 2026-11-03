package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.application.CurrentListingSellerProvider
import com.jdrms.bulletin.domain.listings.application.ListingSeller
import com.jdrms.bulletin.domain.listings.application.ManageListing
import com.jdrms.bulletin.domain.listings.application.ManageListingImages
import com.jdrms.bulletin.domain.listings.attributes.attributeListing
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingImageUpload
import com.jdrms.bulletin.domain.listings.domain.repository.ListingsRepository
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingImagesRepository
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ListingEditActionsTest {
    @Test
    fun failedImageListSaveKeepsEditorOpenWithoutSuccessCallbacks() = runTest {
        val fixture = EditFixture(this, failImageSave = true)
        fixture.repository.createListing(fixture.listing)
        fixture.edit.start(fixture.listing)
        advanceUntilIdle()
        fixture.addPhotoAndSave()
        advanceUntilIdle()

        assertEquals(2, fixture.repository.updateCalls)
        assertNotNull(fixture.state.value.editingListing)
        assertEquals(1, fixture.state.value.editPhotos.size)
        assertFalse(fixture.state.value.isUpdating)
        assertEquals(
            "Listing details were saved, but image changes failed: Image list save failed",
            fixture.state.value.errorMessage
        )
        assertEquals(emptyList(), fixture.notifications)
        assertEquals(0, fixture.reloads)
    }

    @Test
    fun successfulImageListSaveClosesEditorAndRunsSuccessCallbacks() = runTest {
        val fixture = EditFixture(this, failImageSave = false)
        fixture.repository.createListing(fixture.listing)
        fixture.edit.start(fixture.listing)
        advanceUntilIdle()
        fixture.addPhotoAndSave()
        advanceUntilIdle()

        assertEquals(2, fixture.repository.updateCalls)
        assertNull(fixture.state.value.editingListing)
        assertFalse(fixture.state.value.isUpdating)
        assertNull(fixture.state.value.errorMessage)
        assertEquals(listOf("Listing updated successfully!"), fixture.notifications)
        assertEquals(1, fixture.reloads)
    }
}

private class EditFixture(scope: CoroutineScope, failImageSave: Boolean) {
    val listing = attributeListing()
    val repository = ImageSaveRepository(InMemoryListingsRepository(emptyList()), failImageSave)
    val state = MutableStateFlow(ListingsUiState())
    val notifications = mutableListOf<String>()
    var reloads = 0
    private val seller = object : CurrentListingSellerProvider {
        override suspend fun getCurrentSeller() = Result.Success(ListingSeller(listing.sellerId, listing.sellerName))
    }
    val edit = ListingEditActions(
        state,
        scope,
        ManageListing(repository),
        ManageListingImages(InMemoryListingImagesRepository()),
        seller,
        ListingActionCallbacks(null, { notifications.add(it) }, { reloads++ }),
        ListingAttributeDraftActions(state)
    )

    fun addPhotoAndSave() {
        state.value = state.value.copy(
            editPhotos = listOf(ListingPhotoDraft.Pending(ListingImageUpload(byteArrayOf(1), "image/jpeg")))
        )
        edit.save()
    }
}

private class ImageSaveRepository(
    private val delegate: ListingsRepository,
    private val failImageSave: Boolean
) : ListingsRepository by delegate {
    var updateCalls = 0

    override suspend fun updateListing(listing: Listing): Result<Listing> {
        updateCalls++
        return if (failImageSave && updateCalls == 2) {
            Result.Error(IllegalStateException("Image list save failed"))
        } else {
            delegate.updateListing(listing)
        }
    }
}
