package com.jdrms.bulletin.domain.listings.presentation

internal data class NewListingDraftValidation(
    val draft: NewListingDraft?,
    val errorMessage: String?
)

internal fun validateNewListingDraft(state: ListingsUiState): NewListingDraftValidation {
    if (state.newPriceCents.isEmpty()) {
        return NewListingDraftValidation(null, "Please enter a price")
    }
    val title = state.newTitle.trim()
    if (title.length < 3) {
        return NewListingDraftValidation(null, "Title must be at least 3 characters")
    }
    val description = state.newDescription.trim()
    if (description.isBlank()) {
        return NewListingDraftValidation(null, "Description cannot be empty")
    }
    return NewListingDraftValidation(
        NewListingDraft(
            title,
            description,
            currencyDigitsToAmount(state.newPriceCents),
            state.newCategory,
            state.newCondition
        ),
        null
    )
}
