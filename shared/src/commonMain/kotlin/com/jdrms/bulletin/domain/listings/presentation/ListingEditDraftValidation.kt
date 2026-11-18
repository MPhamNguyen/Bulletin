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
    val validation = validateListingForm(
        ListingFormInput(
            title = state.editTitle,
            description = state.editDescription,
            priceCents = state.editPriceCents,
            category = state.editCategory,
            photoCount = state.editPhotos.size,
            attributes = state.editAttributes
        ),
        parser
    )
    val parsed = validation.attributes
    if (!validation.isValid || parsed == null) {
        return EditListingDraftValidation(null, validation.errorMessage, validation.attributeErrors)
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
