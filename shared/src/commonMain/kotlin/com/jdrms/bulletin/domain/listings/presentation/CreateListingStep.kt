package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.domain.listings.domain.service.ListingPhotoPolicy

internal enum class CreateListingStep(val title: String) {
    PHOTOS("Listing basics"),
    DETAILS("Item details"),
    PRICE("Price & description");

    val isFirst: Boolean get() = ordinal == 0
    val isLast: Boolean get() = ordinal == entries.lastIndex
}

/** Whether the seller may leave this step; reuses the submit-time validation so the rules live in one place. */
internal fun CreateListingStep.canContinue(state: ListingsUiState): Boolean = when (this) {
    CreateListingStep.PHOTOS -> state.newTitle.trim().length >= MIN_LISTING_TITLE_LENGTH &&
        ListingPhotoPolicy.hasRequiredPhoto(state.newImages.size)
    CreateListingStep.DETAILS -> true
    CreateListingStep.PRICE -> validateNewListingDraft(state).draft != null
}
