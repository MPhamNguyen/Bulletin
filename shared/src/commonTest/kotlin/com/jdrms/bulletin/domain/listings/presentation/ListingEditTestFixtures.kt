package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.application.CurrentListingSellerProvider
import com.jdrms.bulletin.domain.listings.application.ListingSeller
import com.jdrms.bulletin.domain.listings.application.ManageListing
import com.jdrms.bulletin.domain.listings.application.ManageListingImages
import com.jdrms.bulletin.domain.listings.attributes.attributeListing
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingUpdateIntent
import com.jdrms.bulletin.domain.listings.domain.model.ListingImageUpload
import com.jdrms.bulletin.domain.listings.domain.repository.ListingsRepository
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingImagesRepository
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

internal class ListingEditTestFixture(
    scope: CoroutineScope,
    failImageSave: Boolean = false,
    beforeUpdate: suspend (Listing) -> Result<Listing>? = { null }
) {
    val listing = attributeListing()
    val repository = ImageSaveRepository(InMemoryListingsRepository(emptyList()), failImageSave, beforeUpdate)
    val state = MutableStateFlow(ListingsUiState())
    val notifications = mutableListOf<String>()
    var reloads = 0
    var sellerResult: Result<ListingSeller> = Result.Success(ListingSeller(listing.sellerId, listing.sellerName))
    private val seller = object : CurrentListingSellerProvider {
        override suspend fun getCurrentSeller() = sellerResult
    }
    val edit = ListingEditActions(
        state,
        scope,
        ManageListing(repository),
        ManageListingImages(InMemoryListingImagesRepository()),
        seller,
        ListingActionCallbacks(
            null,
            { message ->
                notifications.add(message)
                state.value = state.value.copy(successMessage = message)
            },
            { reloads++ }
        ),
        ListingAttributeDraftActions(state)
    )

    fun addPhotoAndSave() {
        state.value = state.value.copy(
            editPhotos = listOf(ListingPhotoDraft.Pending(ListingImageUpload(byteArrayOf(1), "image/jpeg")))
        )
        edit.save()
    }
}

internal class ImageSaveRepository(
    private val delegate: ListingsRepository,
    private val failImageSave: Boolean,
    private val beforeUpdate: suspend (Listing) -> Result<Listing>?
) : ListingsRepository by delegate {
    var updateCalls = 0
    val intents = mutableListOf<ListingUpdateIntent>()

    override suspend fun updateListing(listing: Listing, intent: ListingUpdateIntent): Result<Listing> {
        updateCalls++
        intents += intent
        beforeUpdate(listing)?.let { return it }
        return if (failImageSave && updateCalls == 2) {
            Result.Error(IllegalStateException("Image list save failed"))
        } else {
            delegate.updateListing(listing, intent)
        }
    }
}
