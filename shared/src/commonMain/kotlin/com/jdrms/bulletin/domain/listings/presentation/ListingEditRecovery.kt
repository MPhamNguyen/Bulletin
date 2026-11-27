package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.domain.listings.domain.model.Listing

/** Restore only the failed edit's form, leaving other listing flows untouched. */
internal fun ListingsUiState.restoreEditDraft(form: ListingsUiState, persisted: Listing): ListingsUiState = copy(
    editingListing = persisted,
    editTitle = form.editTitle,
    editDescription = form.editDescription,
    editPriceCents = form.editPriceCents,
    editCategory = form.editCategory,
    editCondition = form.editCondition,
    editPhotos = form.editPhotos,
    editAttributes = form.editAttributes,
    pendingCategoryChange = form.pendingCategoryChange
)
