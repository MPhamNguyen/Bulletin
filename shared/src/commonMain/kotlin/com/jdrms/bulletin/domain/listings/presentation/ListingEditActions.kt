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
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeInputFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The edit-listing flow: loading a listing into the form, resetting it, and saving changes. */
internal class ListingEditActions(
    private val state: MutableStateFlow<ListingsUiState>,
    private val scope: CoroutineScope,
    private val manageListing: ManageListing,
    private val manageImages: ManageListingImages?,
    private val sellerProvider: CurrentListingSellerProvider,
    private val callbacks: ListingActionCallbacks,
    private val attributeActions: ListingAttributeDraftActions
) {
    fun start(listing: Listing) {
        callbacks.cancelNotification()
        scope.launch {
            when (val sellerResult = sellerProvider.getCurrentSeller()) {
                is Result.Success -> load(listing, sellerResult.data)
                is Result.Error -> state.update {
                    it.copy(errorMessage = CreateListingErrorMessages.toUserMessage(sellerResult.exception))
                }
            }
        }
    }

    private fun load(listing: Listing, seller: ListingSeller) {
        if (!listing.isOwnedBy(seller.id)) {
            state.update { it.copy(errorMessage = "Only the owner can edit this listing.") }
            return
        }
        state.update {
            it.copy(
                editingListing = listing,
                editTitle = listing.title,
                editDescription = listing.description,
                editPriceCents = amountToCurrencyDigits(listing.price.amount),
                editCategory = listing.category,
                editCondition = listing.condition,
                editPhotos = listing.images.map { image -> ListingPhotoDraft.Existing(image) },
                editAttributes = ListingAttributeDraft(
                    inputs = ListingAttributeInputFormatter.inputsFor(listing.attributes),
                    preserved = listing.attributes.preserved
                ),
                pendingCategoryChange = null,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun cancel() {
        callbacks.cancelNotification()
        state.update {
            it.copy(
                editingListing = null,
                editTitle = "",
                editDescription = "",
                editPriceCents = "",
                editPhotos = emptyList(),
                editAttributes = ListingAttributeDraft(),
                pendingCategoryChange = null,
                isUpdating = false,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun save() {
        val draft = validate() ?: return
        while (true) {
            val current = state.value
            if (current.isUpdating) return
            if (state.compareAndSet(current, current.copy(isUpdating = true, errorMessage = null))) break
        }
        scope.launch { apply(draft) }
    }

    private fun validate(): ValidatedEditDraft? {
        val validation = validateEditListingDraft(state.value)
        validation.errorMessage?.let { message -> state.update { it.copy(errorMessage = message) } }
        if (validation.draft != null || validation.errorMessage != null) {
            attributeActions.showErrors(ListingForm.EDIT, validation.attributeErrors)
        }
        return validation.draft
    }

    private suspend fun apply(draft: ValidatedEditDraft) {
        when (val sellerResult = sellerProvider.getCurrentSeller()) {
            is Result.Success -> {
                val seller = sellerResult.data
                if (!draft.currentListing.isOwnedBy(seller.id)) {
                    state.update { it.copy(isUpdating = false, errorMessage = "Only the owner can edit this listing.") }
                    return
                }
                execute(draft, seller)
            }
            is Result.Error -> state.update {
                it.copy(
                    isUpdating = false,
                    errorMessage = CreateListingErrorMessages.toUserMessage(sellerResult.exception)
                )
            }
        }
    }

    private suspend fun execute(draft: ValidatedEditDraft, seller: ListingSeller) {
        val updatedDetails = runCatching {
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
        }
        if (updatedDetails.isFailure) {
            state.update {
                it.copy(
                    isUpdating = false,
                    errorMessage = updatedDetails.exceptionOrNull()?.message ?: "Validation failed"
                )
            }
            return
        }
        val updated = updatedDetails.getOrThrow()
        when (val result = manageListing.updateListing(updated, seller.id)) {
            is Result.Success -> complete(draft, seller, updated)
            is Result.Error -> state.update {
                it.copy(isUpdating = false, errorMessage = CreateListingErrorMessages.toUserMessage(result.exception))
            }
        }
    }

    private suspend fun complete(draft: ValidatedEditDraft, seller: ListingSeller, updated: Listing) {
        if (!synchronizeImages(draft, seller, updated)) return
        state.update {
            it.copy(editingListing = null, editAttributes = ListingAttributeDraft(), isUpdating = false)
        }
        callbacks.notify("Listing updated successfully!")
        callbacks.reload(seller)
        callbacks.listingChangedSignal?.emit()
    }

    private suspend fun synchronizeImages(draft: ValidatedEditDraft, seller: ListingSeller, listing: Listing): Boolean {
        val retained = draft.photos.filterIsInstance<ListingPhotoDraft.Existing>().map { it.url }
        val uploads = draft.photos.filterIsInstance<ListingPhotoDraft.Pending>().map { it.upload }
        if (draft.currentListing.images.isEmpty() && uploads.isEmpty()) return true
        val result = manageImages?.synchronize(listing.id, seller.id, retained, uploads)
            ?: Result.Error(IllegalStateException("Listing image updates are not configured."))
        val updateResult = when (result) {
            is Result.Success -> manageListing.updateListing(listing.copy(images = result.data), seller.id)
            is Result.Error -> result
        }
        return when (updateResult) {
            is Result.Success -> true
            is Result.Error -> {
                state.update {
                    it.copy(
                        isUpdating = false,
                        errorMessage = "Listing details were saved, but image changes failed: " +
                            updateResult.exception.message
                    )
                }
                false
            }
        }
    }
}
