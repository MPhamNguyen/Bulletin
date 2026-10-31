package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeDate
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeDefinition
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeInputParser

private const val DATE_TEXT_LENGTH = 10

internal fun parseDateInput(text: String): ListingAttributeDate? = ListingAttributeDate.parse(text)

internal fun dateInputError(
    parser: ListingAttributeInputParser,
    category: ListingCategory,
    definition: ListingAttributeDefinition,
    text: String
): String? = if (text.length < DATE_TEXT_LENGTH) {
    null
} else {
    parser.parse(category, mapOf(definition.key to text)).errors[definition.key]
}
