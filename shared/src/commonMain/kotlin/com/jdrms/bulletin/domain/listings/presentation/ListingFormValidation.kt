package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributes
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeInputParser
import com.jdrms.bulletin.domain.listings.domain.service.ListingPhotoPolicy

internal const val MIN_LISTING_TITLE_LENGTH = 3

internal const val ATTRIBUTE_ERRORS_MESSAGE = "Fix the highlighted item details."

/** Form values every listing form (create and edit) is validated on. */
internal data class ListingFormInput(
    val title: String,
    val description: String,
    val priceCents: String,
    val category: ListingCategory,
    val photoCount: Int,
    val attributes: ListingAttributeDraft
)

internal data class ListingFormValidation(
    val attributes: ListingAttributes?,
    val errorMessage: String?,
    val attributeErrors: Map<String, String> = emptyMap()
) {
    val isValid: Boolean get() = errorMessage == null && attributes != null
}

/** The single source of truth for which listing fields are required; create and edit both delegate here. */
internal fun validateListingForm(
    input: ListingFormInput,
    parser: ListingAttributeInputParser = ListingAttributeInputParser()
): ListingFormValidation {
    val parsed = parseListingFormAttributes(input.category, input.attributes, input.priceCents, parser)
    val baseError = when {
        input.priceCents.isEmpty() -> if (input.category == ListingCategory.HOUSING) {
            "Please enter the monthly rent"
        } else {
            "Please enter a price"
        }
        parsed.rentError != null -> parsed.rentError
        input.title.trim().length < MIN_LISTING_TITLE_LENGTH ->
            "Title must be at least $MIN_LISTING_TITLE_LENGTH characters"
        input.description.isBlank() -> "Description cannot be empty"
        !ListingPhotoPolicy.hasRequiredPhoto(input.photoCount) -> ListingPhotoPolicy.REQUIRED_PHOTO_MESSAGE
        else -> null
    }
    val attributes = parsed.attributes
    if (baseError != null || attributes == null) {
        return ListingFormValidation(null, baseError ?: ATTRIBUTE_ERRORS_MESSAGE, parsed.errors)
    }
    return ListingFormValidation(attributes, null)
}
