package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.designsystem.amountToCurrencyDigits
import com.jdrms.bulletin.domain.listings.application.CreateListingErrorMessages
import com.jdrms.bulletin.domain.listings.application.CurrentListingSellerProvider
import com.jdrms.bulletin.domain.listings.application.ListingSeller
import com.jdrms.bulletin.domain.listings.application.ManageListing
import com.jdrms.bulletin.domain.listings.application.ManageListingImages
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeKeys
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeInputFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The edit-listing flow: loading a listing into the form, resetting it, and saving changes. */
internal class ListingEditActions(
    private val state: MutableStateFlow<ListingsUiState>,
    private val scope: CoroutineScope,
    manageListing: ManageListing,
    manageImages: ManageListingImages?,
    private val sellerProvider: CurrentListingSellerProvider,
    private val callbacks: ListingActionCallbacks,
    private val attributeActions: ListingAttributeDraftActions
) {
    private var editor: ListingSeller? = null
    private val submission = ListingEditSubmission(state, scope, manageListing, manageImages, sellerProvider, callbacks)

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
        editor = seller
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
                    inputs = ListingAttributeInputFormatter.inputsFor(listing.attributes)
                        .minus(ListingAttributeKeys.MONTHLY_RENT_USD),
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
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun save() {
        val draft = validate() ?: return
        val seller = editor ?: return
        submission.submit(draft, seller)
    }

    private fun validate(): ValidatedEditDraft? {
        val validation = validateEditListingDraft(state.value)
        validation.errorMessage?.let { message -> state.update { it.copy(errorMessage = message) } }
        if (validation.draft != null || validation.errorMessage != null) {
            attributeActions.showErrors(ListingForm.EDIT, validation.attributeErrors)
        }
        return validation.draft
    }
}
