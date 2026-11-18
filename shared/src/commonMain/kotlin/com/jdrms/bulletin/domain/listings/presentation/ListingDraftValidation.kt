package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.core.designsystem.currencyDigitsToAmount
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeInputParser

internal data class NewListingDraftValidation(
    val draft: NewListingDraft?,
    val errorMessage: String?,
    val attributeErrors: Map<String, String> = emptyMap()
)

internal fun validateNewListingDraft(
    state: ListingsUiState,
    parser: ListingAttributeInputParser = ListingAttributeInputParser()
): NewListingDraftValidation {
    val validation = validateListingForm(
        ListingFormInput(
            title = state.newTitle,
            description = state.newDescription,
            priceCents = state.newPriceCents,
            category = state.newCategory,
            photoCount = state.newImages.size,
            attributes = state.newAttributes
        ),
        parser
    )
    val parsed = validation.attributes
    if (!validation.isValid || parsed == null) {
        return NewListingDraftValidation(null, validation.errorMessage, validation.attributeErrors)
    }
    return NewListingDraftValidation(
        NewListingDraft(
            state.newTitle.trim(),
            state.newDescription.trim(),
            currencyDigitsToAmount(state.newPriceCents),
            state.newCategory,
            state.newCondition,
            state.newImages,
            parsed
        ),
        null
    )
}
