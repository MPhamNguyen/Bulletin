package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeDate
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeDefinition
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeInputParser

internal const val DATE_INPUT_DIGITS = 8
private const val DATE_TEXT_LENGTH = 10
private const val YEAR_DIGITS = 4
private const val MONTH_END_DIGITS = 6

internal fun dateDigits(raw: String): String = raw.filter { it in '0'..'9' }.take(DATE_INPUT_DIGITS)

internal fun formatDateDigits(digits: String): String = buildString {
    digits.forEachIndexed { index, digit ->
        if (index == YEAR_DIGITS || index == MONTH_END_DIGITS) append('-')
        append(digit)
    }
}

internal fun formattedCursorOffset(digitsBefore: Int): Int =
    digitsBefore + (if (digitsBefore > YEAR_DIGITS) 1 else 0) + (if (digitsBefore > MONTH_END_DIGITS) 1 else 0)

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
