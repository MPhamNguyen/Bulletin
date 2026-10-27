package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.core.common.generateUuid
import com.jdrms.bulletin.domain.listings.application.CreateListing
import com.jdrms.bulletin.domain.listings.application.CreateListingErrorMessages
import com.jdrms.bulletin.domain.listings.application.CurrentListingSellerProvider
import com.jdrms.bulletin.domain.listings.application.ListingSeller
import com.jdrms.bulletin.domain.listings.application.ManageListing
import com.jdrms.bulletin.domain.listings.application.ManageListingImages
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.ListingImageUpload
import com.jdrms.bulletin.domain.listings.domain.model.ListingPrice
import com.jdrms.bulletin.domain.listings.domain.model.ListingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ListingCreationActions(
    private val state: MutableStateFlow<ListingsUiState>,
    private val scope: CoroutineScope,
    private val createListing: CreateListing,
    private val manageListing: ManageListing,
    private val manageImages: ManageListingImages?,
    private val sellerProvider: CurrentListingSellerProvider,
    private val callbacks: ListingActionCallbacks
) {
    fun submit() {
        val draft = buildDraft() ?: return
        while (true) {
            val current = state.value
            if (current.isSubmitting) return
            if (state.compareAndSet(current, current.copy(isSubmitting = true, errorMessage = null))) break
        }
        scope.launch { submit(draft) }
    }

    private fun buildDraft(): NewListingDraft? {
        val validation = validateNewListingDraft(state.value)
        validation.errorMessage?.let { message -> state.update { it.copy(errorMessage = message) } }
        return validation.draft
    }

    private suspend fun submit(draft: NewListingDraft) {
        when (val seller = sellerProvider.getCurrentSeller()) {
            is Result.Success -> createForSeller(draft, seller.data)
            is Result.Error -> state.update {
                it.copy(isSubmitting = false, errorMessage = CreateListingErrorMessages.toUserMessage(seller.exception))
            }
        }
    }

    private suspend fun createForSeller(draft: NewListingDraft, seller: ListingSeller) {
        val listing = Listing(
            id = ListingId(generateUuid()),
            sellerId = seller.id,
            sellerName = seller.name,
            title = draft.title,
            description = draft.description,
            price = ListingPrice(draft.price),
            category = draft.category,
            condition = draft.condition,
            status = ListingStatus.AVAILABLE,
            createdAtMillis = currentTimeMillis()
        )
        when (val result = createListing(listing)) {
            is Result.Success -> complete(seller, listing, draft.images)
            is Result.Error -> state.update {
                it.copy(isSubmitting = false, errorMessage = CreateListingErrorMessages.toUserMessage(result.exception))
            }
        }
    }

    private suspend fun complete(
        seller: ListingSeller,
        listing: Listing,
        images: List<ListingImageUpload>
    ) {
        val imageResult = if (images.isEmpty()) {
            Result.Success(emptyList())
        } else {
            manageImages?.synchronize(listing.id, seller.id, emptyList(), images)
                ?: Result.Error(IllegalStateException("Listing image uploads are not configured."))
        }
        if (imageResult is Result.Success && imageResult.data.isNotEmpty()) {
            manageListing.updateListing(listing.copy(images = imageResult.data), seller.id)
        }
        val warning = (imageResult as? Result.Error)?.exception?.message
        state.update {
            it.copy(
                newTitle = "",
                newDescription = "",
                newPriceCents = "",
                newImages = emptyList(),
                isSubmitting = false,
                errorMessage = warning
            )
        }
        callbacks.notify(
            if (warning == null) {
                "Listing posted successfully!"
            } else {
                "Listing posted, but its images could not be uploaded. Edit the listing to retry."
            }
        )
        callbacks.reload(seller)
        callbacks.listingChangedSignal?.emit()
    }
}
