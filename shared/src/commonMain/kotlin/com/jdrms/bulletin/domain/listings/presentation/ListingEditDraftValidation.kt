package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.core.designsystem.currencyDigitsToAmount
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeInputParser

internal data class EditListingDraftValidation(
    val draft: ValidatedEditDraft?,
    val errorMessage: String?,
    val attributeErrors: Map<String, String> = emptyMap()
)

internal fun validateEditListingDraft(
    state: ListingsUiState,
    parser: ListingAttributeInputParser = ListingAttributeInputParser()
): EditListingDraftValidation {
    val currentListing = state.editingListing ?: return EditListingDraftValidation(null, null)
    val attributes = parser.parse(state.editCategory, state.editAttributes.inputs, state.editAttributes.preserved)
    val baseError = when {
        state.editPriceCents.isEmpty() -> "Please enter a price"
        state.editTitle.trim().length < 3 -> "Title must be at least 3 characters"
        state.editDescription.trim().isBlank() -> "Description cannot be empty"
        else -> null
    }
    val parsed = attributes.attributes
    if (baseError != null || parsed == null) {
        return EditListingDraftValidation(null, baseError ?: ATTRIBUTE_ERRORS_MESSAGE, attributes.errors)
    }
    return EditListingDraftValidation(
        ValidatedEditDraft(
            currentListing = currentListing,
            title = state.editTitle.trim(),
            description = state.editDescription.trim(),
            price = currencyDigitsToAmount(state.editPriceCents),
            category = state.editCategory,
            condition = state.editCondition,
            photos = state.editPhotos,
            attributes = parsed
        ),
        null
    )
}
