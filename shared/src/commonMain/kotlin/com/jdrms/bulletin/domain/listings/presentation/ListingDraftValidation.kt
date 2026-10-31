package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.core.designsystem.currencyDigitsToAmount
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeInputParser
import com.jdrms.bulletin.domain.listings.domain.service.ListingPhotoPolicy

internal const val MIN_LISTING_TITLE_LENGTH = 3

internal const val ATTRIBUTE_ERRORS_MESSAGE = "Fix the highlighted item details."

internal data class NewListingDraftValidation(
    val draft: NewListingDraft?,
    val errorMessage: String?,
    val attributeErrors: Map<String, String> = emptyMap()
)

internal fun validateNewListingDraft(
    state: ListingsUiState,
    parser: ListingAttributeInputParser = ListingAttributeInputParser()
): NewListingDraftValidation {
    val attributes = parser.parse(state.newCategory, state.newAttributes.inputs, state.newAttributes.preserved)
    val title = state.newTitle.trim()
    val description = state.newDescription.trim()
    val baseError = when {
        state.newPriceCents.isEmpty() -> "Please enter a price"
        title.length < MIN_LISTING_TITLE_LENGTH -> "Title must be at least $MIN_LISTING_TITLE_LENGTH characters"
        description.isBlank() -> "Description cannot be empty"
        !ListingPhotoPolicy.hasRequiredPhoto(state.newImages.size) -> ListingPhotoPolicy.REQUIRED_PHOTO_MESSAGE
        else -> null
    }
    val parsed = attributes.attributes
    if (baseError != null || parsed == null) {
        return NewListingDraftValidation(null, baseError ?: ATTRIBUTE_ERRORS_MESSAGE, attributes.errors)
    }
    return NewListingDraftValidation(
        NewListingDraft(
            title,
            description,
            currencyDigitsToAmount(state.newPriceCents),
            state.newCategory,
            state.newCondition,
            state.newImages,
            parsed
        ),
        null
    )
}
