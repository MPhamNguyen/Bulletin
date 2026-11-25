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
    private val callbacks: ListingActionCallbacks,
    private val attributeActions: ListingAttributeDraftActions
) {
    /**
     * Posts optimistically: the form is cleared and the caller can close the create flow right away, while the
     * listing is saved in the background. Returns false (and posts nothing) when the draft is invalid. A failed
     * save removes the optimistic listing and puts the form back.
     */
    fun submit(): Boolean {
        val draft = buildDraft() ?: return false
        val form = state.value
        state.update {
            it.copy(
                newTitle = "",
                newDescription = "",
                newPriceCents = "",
                newImages = emptyList(),
                newAttributes = ListingAttributeDraft(),
                errorMessage = null
            )
        }
        callbacks.notify(POSTED_MESSAGE)
        scope.launch { submit(draft, form) }
        return true
    }

    private fun buildDraft(): NewListingDraft? {
        val validation = validateNewListingDraft(state.value)
        validation.errorMessage?.let { message -> state.update { it.copy(errorMessage = message) } }
        attributeActions.showErrors(ListingForm.NEW, validation.attributeErrors)
        return validation.draft
    }

    private suspend fun submit(draft: NewListingDraft, form: ListingsUiState) {
        when (val seller = sellerProvider.getCurrentSeller()) {
            is Result.Success -> createForSeller(draft, seller.data, form)
            is Result.Error -> rollBack(form, null, CreateListingErrorMessages.toUserMessage(seller.exception))
        }
    }

    private suspend fun createForSeller(draft: NewListingDraft, seller: ListingSeller, form: ListingsUiState) {
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
            createdAtMillis = currentTimeMillis(),
            attributes = draft.attributes
        )
        state.update { it.copy(myListings = listOf(listing) + it.myListings) }
        when (val result = createListing(listing)) {
            is Result.Success -> complete(seller, listing, draft.images)
            is Result.Error -> rollBack(form, listing.id, CreateListingErrorMessages.toUserMessage(result.exception))
        }
    }

    /** Drops the optimistic listing and restores the form, unless the seller has already started a new draft. */
    private fun rollBack(form: ListingsUiState, listingId: ListingId?, message: String) {
        state.update { current ->
            val formIsFree = current.newTitle.isEmpty() && current.newDescription.isEmpty() &&
                current.newPriceCents.isEmpty() && current.newImages.isEmpty()
            val restored = if (formIsFree) {
                current.copy(
                    newTitle = form.newTitle,
                    newDescription = form.newDescription,
                    newPriceCents = form.newPriceCents,
                    newImages = form.newImages,
                    newAttributes = form.newAttributes
                )
            } else {
                current
            }
            restored.copy(
                myListings = restored.myListings.filterNot { it.id == listingId },
                successMessage = null,
                errorMessage = message
            )
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
        state.update { it.copy(errorMessage = warning) }
        if (warning != null) {
            callbacks.notify("Listing posted, but its images could not be uploaded. Edit the listing to retry.")
        }
        callbacks.reload(seller)
        callbacks.listingChangedSignal?.emit()
    }

    private companion object {
        const val POSTED_MESSAGE = "Listing Posted"
    }
}
