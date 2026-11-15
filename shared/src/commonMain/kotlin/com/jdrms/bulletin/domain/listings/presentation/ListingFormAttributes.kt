package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeKeys
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributes
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeInputParser

internal data class ListingFormAttributes(
    val attributes: ListingAttributes?,
    val errors: Map<String, String>,
    val rentError: String?
)

/** Housing has one amount input; its price also supplies the persisted rent attribute used by buyer filters. */
internal fun parseListingFormAttributes(
    category: ListingCategory,
    draft: ListingAttributeDraft,
    priceCents: String,
    parser: ListingAttributeInputParser = ListingAttributeInputParser()
): ListingFormAttributes {
    val rentKey = ListingAttributeKeys.MONTHLY_RENT_USD
    val housing = category == ListingCategory.HOUSING
    val inputs = if (housing) draft.inputs + (rentKey to rentDigitsToAmount(priceCents)) else draft.inputs
    val parsed = parser.parse(category, inputs, draft.preserved)
    return ListingFormAttributes(
        parsed.attributes,
        if (housing) parsed.errors - rentKey else parsed.errors,
        if (housing) parsed.errors[rentKey] else null
    )
}
