package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeDefinition
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeType
import com.jdrms.bulletin.domain.listings.domain.model.ListingTextFormat

private const val CLOTHING_SIZE_PUNCTUATION = " ./-"
private const val COURSE_CODE_SEPARATORS = " -"

/** Filters characters a field cannot accept as the seller types; domain parsing remains authoritative. */
internal fun ListingAttributeDefinition.filterInput(raw: String): String = when (val type = type) {
    is ListingAttributeType.Integer -> filterWholeNumber(raw, type.max.toString().length)
    is ListingAttributeType.Decimal -> {
        val wholeDigits = type.max.toPlainString().substringBefore('.').length
        filterDecimal(raw, wholeDigits, type.scale)
    }
    is ListingAttributeType.Text -> filterText(type.format, raw, type.maxLength)
    is ListingAttributeType.Choice, is ListingAttributeType.Date -> raw
}

private fun Char.isAsciiDigit() = this in '0'..'9'

private fun Char.isAsciiLetter() = this in 'a'..'z' || this in 'A'..'Z'

private fun filterWholeNumber(raw: String, maxDigits: Int): String = raw.filter(Char::isAsciiDigit).take(maxDigits)

/** Digits with one decimal point; a comma is read as the point only when no point was typed. */
private fun filterDecimal(raw: String, maxWholeDigits: Int, scale: Int): String {
    val separator = if ('.' in raw) '.' else ','
    val out = StringBuilder()
    var pointSeen = false
    var wholeDigits = 0
    var fractionDigits = 0
    for (char in raw) {
        when {
            char.isAsciiDigit() && !pointSeen && wholeDigits < maxWholeDigits -> { out.append(char); wholeDigits++ }
            char.isAsciiDigit() && pointSeen && fractionDigits < scale -> { out.append(char); fractionDigits++ }
            char == separator && !pointSeen && scale > 0 -> { out.append('.'); pointSeen = true }
        }
    }
    return out.toString()
}

private fun filterText(format: ListingTextFormat, raw: String, maxLength: Int): String = when (format) {
    ListingTextFormat.ISBN -> formatIsbnInput(raw)
    ListingTextFormat.COURSE_CODE ->
        raw.filter { it.isAsciiLetter() || it.isAsciiDigit() || it in COURSE_CODE_SEPARATORS }.take(maxLength)
    ListingTextFormat.CLOTHING_SIZE -> raw.uppercase()
        .filter { it.isAsciiLetter() || it.isAsciiDigit() || it in CLOTHING_SIZE_PUNCTUATION }.take(maxLength)
    ListingTextFormat.BRAND -> raw.filterNot(Char::isISOControl).take(maxLength)
}
