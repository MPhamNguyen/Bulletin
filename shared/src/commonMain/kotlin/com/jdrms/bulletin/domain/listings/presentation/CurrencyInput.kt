package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.jdrms.bulletin.domain.listings.domain.model.MAX_LISTING_PRICE_CENTS
import kotlin.math.round

internal const val MAX_CURRENCY_DIGITS = 9

/**
 * Keeps only ASCII cents digits so a currency field can behave like a cash-register keypad.
 * Empty and all-zero input represent an unset price.
 */
internal fun normalizeCurrencyDigits(input: String): String {
    return input
        .filter { it in '0'..'9' }
        .trimStart('0')
        .take(MAX_CURRENCY_DIGITS)
}

/** Formats normalized cents digits as USD, or returns an empty string for an unset price. */
internal fun formatCurrencyDigits(digits: String): String {
    val normalized = normalizeCurrencyDigits(digits)
    if (normalized.isEmpty()) return ""
    val cents = normalized.toLong()
    val dollars = (cents / 100).toString().reversed().chunked(3).joinToString(",").reversed()
    val remainder = (cents % 100).toString().padStart(2, '0')
    return buildString {
        append('$')
        append(dollars)
        append('.')
        append(remainder)
    }
}

/** Converts normalized cents digits to a decimal amount. Empty input maps to zero for domain construction. */
internal fun currencyDigitsToAmount(digits: String): Double =
    normalizeCurrencyDigits(digits).toLongOrNull()?.let { it / 100.0 } ?: 0.0

/** Converts a valid, non-negative amount to cents digits; zero is represented as unset. */
internal fun amountToCurrencyDigits(amount: Double): String {
    require(amount.isFinite() && amount >= 0.0) { "Currency amount must be finite and non-negative." }
    val cents = round(amount * 100).toLong()
    require(cents <= MAX_LISTING_PRICE_CENTS) { "Currency amount exceeds the listing price limit." }
    return cents.takeIf { it > 0 }?.toString().orEmpty()
}

/**
 * Interprets a text-field change while preserving cents-first typing. Pasted decimal amounts are
 * parsed as amounts so that, for example, `12.5` means $12.50 rather than $1.25.
 */
internal fun normalizeCurrencyTextInput(input: String, previousText: String): String {
    return normalizeCurrencyTextChange(
        newValue = TextFieldValue(input, selection = TextRange(input.length)),
        previousValue = TextFieldValue(previousText, selection = TextRange(previousText.length))
    )
}

/** Applies an edit while treating the formatted currency text as an end-pinned keypad. */
internal fun normalizeCurrencyTextChange(
    newValue: TextFieldValue,
    previousValue: TextFieldValue
): String {
    if (!previousValue.selection.collapsed) {
        val selectedAll = previousValue.selection.start == 0 &&
            previousValue.selection.end == previousValue.text.length
        return when {
            selectedAll -> normalizeCurrencyReplacement(newValue.text)
            newValue.text.length < previousValue.text.length ->
                normalizeCurrencyDigits(normalizeCurrencyDigits(previousValue.text).dropLast(1))
            else -> normalizeCurrencyReplacement(newValue.text)
        }
    }

    val previousText = previousValue.text
    val changedSegment = changedText(previousText, newValue.text)
    return when {
        newValue.text.length < previousText.length ->
            normalizeCurrencyDigits(normalizeCurrencyDigits(previousText).dropLast(1))
        newValue.text.length == previousText.length -> normalizeCurrencyDigits(newValue.text)
        changedSegment != null -> normalizeCurrencyInsertion(changedSegment, previousText)
        else -> normalizeCurrencyReplacement(newValue.text)
    }
}

private fun normalizeCurrencyInsertion(insertedText: String, previousText: String): String {
    val pastedAmount = insertedText.replace("$", "").replace(",", "")
    if (pastedAmount.contains('.') && pastedAmount.toDoubleOrNull()?.isFinite() == true) {
        return amountToCurrencyDigits(pastedAmount.toDouble())
    }
    return normalizeCurrencyDigits(normalizeCurrencyDigits(previousText) + insertedText)
}

internal fun normalizeCurrencyReplacement(input: String): String {
    val pastedAmount = input.replace("$", "").replace(",", "")
    return if (pastedAmount.contains('.') && pastedAmount.toDoubleOrNull()?.isFinite() == true) {
        amountToCurrencyDigits(pastedAmount.toDouble())
    } else {
        normalizeCurrencyDigits(input)
    }
}

private fun changedText(previousText: String, newText: String): String? {
    var prefixLength = 0
    while (
        prefixLength < previousText.length &&
        prefixLength < newText.length &&
        previousText[prefixLength] == newText[prefixLength]
    ) {
        prefixLength++
    }

    var suffixLength = 0
    while (
        suffixLength < previousText.length - prefixLength &&
        suffixLength < newText.length - prefixLength &&
        previousText[previousText.length - suffixLength - 1] == newText[newText.length - suffixLength - 1]
    ) {
        suffixLength++
    }

    val insertedEnd = newText.length - suffixLength
    return if (insertedEnd >= prefixLength) newText.substring(prefixLength, insertedEnd) else null
}
