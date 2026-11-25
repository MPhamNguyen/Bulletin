package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.domain.listings.application.CreateListingErrorMessages
import com.jdrms.bulletin.domain.listings.application.CurrentListingSellerProvider
import com.jdrms.bulletin.domain.listings.application.ListingSeller
import com.jdrms.bulletin.domain.listings.application.ManageListing
import com.jdrms.bulletin.domain.listings.application.ManageListingImages
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingDetails
import com.jdrms.bulletin.domain.listings.domain.model.ListingPrice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Owns an optimistic edit until both its details and photos have finished saving. */
internal class ListingEditSubmission(
    private val state: MutableStateFlow<ListingsUiState>,
    private val scope: CoroutineScope,
    private val manageListing: ManageListing,
    private val manageImages: ManageListingImages?,
    private val sellerProvider: CurrentListingSellerProvider,
    private val callbacks: ListingActionCallbacks
) {
    private var pending = false

    fun submit(draft: ValidatedEditDraft, seller: ListingSeller) {
        if (pending) return
        val updated = preview(draft, seller) ?: return
        val form = state.value
        pending = true
        state.update {
            it.copy(
                editingListing = null,
                editTitle = "",
                editDescription = "",
                editPriceCents = "",
                editPhotos = emptyList(),
                editAttributes = ListingAttributeDraft(),
                pendingCategoryChange = null,
                isUpdating = true,
                errorMessage = null,
                myListings = it.myListings.map { listing -> if (listing.id == updated.id) updated else listing }
            )
        }
        callbacks.notify(UPDATED_MESSAGE)
        scope.launch {
            try {
                save(draft, updated, form)
            } finally {
                pending = false
                state.update { it.copy(isUpdating = false) }
            }
        }
    }

    private fun preview(draft: ValidatedEditDraft, seller: ListingSeller): Listing? = runCatching {
        draft.currentListing.updateDetails(
            editorSellerId = seller.id,
            details = ListingDetails(
                title = draft.title,
                description = draft.description,
                price = ListingPrice(draft.price),
                category = draft.category,
                condition = draft.condition,
                images = draft.currentListing.images,
                attributes = draft.attributes
            ),
            updatedAtMillis = currentTimeMillis()
        )
    }.getOrElse { error ->
        state.update { it.copy(errorMessage = error.message ?: "Validation failed") }
        null
    }

    private suspend fun save(draft: ValidatedEditDraft, updated: Listing, form: ListingsUiState) {
        val seller = when (val result = sellerProvider.getCurrentSeller()) {
            is Result.Success -> result.data
            is Result.Error -> {
                recover(form, updated, draft.currentListing, CreateListingErrorMessages.toUserMessage(result.exception))
                return
            }
        }
        when (val result = manageListing.saveEdit(updated, seller.id)) {
            is Result.Success -> complete(draft, seller, result.data, updated, form)
            is Result.Error -> recover(
                form,
                updated,
                draft.currentListing,
                CreateListingErrorMessages.toUserMessage(result.exception)
            )
        }
    }

    private suspend fun complete(
        draft: ValidatedEditDraft,
        seller: ListingSeller,
        saved: Listing,
        optimistic: Listing,
        form: ListingsUiState
    ) {
        when (val result = synchronizeEditedListingImages(draft, seller, saved, manageImages, manageListing)) {
            is Result.Success -> state.update { current ->
                current.copy(myListings = current.myListings.map { if (it == optimistic) result.data else it })
            }
            is Result.Error -> recover(
                form,
                optimistic,
                saved,
                "Listing details were saved, but image changes failed: " + result.exception.message
            )
        }
        callbacks.reload(seller)
        callbacks.listingChangedSignal?.emit()
    }

    private fun recover(form: ListingsUiState, optimistic: Listing, persisted: Listing, message: String) {
        state.update { current ->
            val restored = if (current.editingListing == null) current.restoreEditDraft(form, persisted) else current
            restored.copy(
                myListings = restored.myListings.map { if (it == optimistic) persisted else it },
                errorMessage = message,
                successMessage = restored.successMessage.takeUnless { it == UPDATED_MESSAGE }
            )
        }
    }

    private companion object {
        const val UPDATED_MESSAGE = "Listing updated successfully!"
    }
}
