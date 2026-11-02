package com.jdrms.bulletin.core.designsystem

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.jdrms.bulletin.core.common.Result

private val currencyAmountPattern = Regex("(?:[0-9]+(?:\\.[0-9]{0,2})?|\\.[0-9]{1,2})")

/** Preserves cents-first typing; pasted decimals such as `12.5` mean $12.50. */
internal fun normalizeCurrencyTextInput(input: String, previousText: String): Result<String> =
    normalizeCurrencyTextChange(
        newValue = TextFieldValue(input, selection = TextRange(input.length)),
        previousValue = TextFieldValue(previousText, selection = TextRange(previousText.length))
    )

/** Invalid edits return an error so callers can retain the accepted value and explain the rejection. */
internal fun normalizeCurrencyTextChange(
    newValue: TextFieldValue,
    previousValue: TextFieldValue
): Result<String> {
    val previousText = previousValue.text
    if (newValue.text == previousText) return Result.Success(normalizeCurrencyDigits(previousText))
    val selectedAll = !previousValue.selection.collapsed && previousValue.selection.min == 0 &&
        previousValue.selection.max == previousText.length
    if (selectedAll) return normalizeCurrencyReplacement(newValue.text)

    val changedSegment = changedText(previousText, newValue.text)
    return when {
        changedSegment.isEmpty() ->
            Result.Success(normalizeCurrencyDigits(normalizeCurrencyDigits(previousText).dropLast(1)))
        !previousValue.selection.collapsed || newValue.text.length == previousText.length ->
            normalizeCurrencyReplacement(newValue.text)
        else -> normalizeCurrencyInsertion(changedSegment, previousText)
    }
}

private fun normalizeCurrencyInsertion(insertedText: String, previousText: String): Result<String> {
    val insertion = normalizeCurrencyReplacement(insertedText)
    return if (insertion is Result.Success && !insertedText.contains('.')) {
        val insertedDigits = insertedText.trim().removePrefix("$").replace(",", "")
        normalizeCurrencyReplacement(normalizeCurrencyDigits(previousText) + insertedDigits)
    } else {
        insertion
    }
}

/** Parses cents digits or a decimal amount without rounding, truncating, or constructing a domain price. */
internal fun normalizeCurrencyReplacement(input: String): Result<String> {
    val amount = input.trim().removePrefix("$").replace(",", "")
    val error = when {
        '-' in amount -> "Amount cannot be negative."
        amount.isNotEmpty() && !currencyAmountPattern.matches(amount) ->
            "Enter a valid amount with up to two decimal places."
        else -> null
    }
    if (error != null) return Result.Error(IllegalArgumentException(error))
    if (amount.isEmpty()) return Result.Success("")
    val cents = if ('.' in amount) {
        amount.substringBefore('.') + amount.substringAfter('.').padEnd(2, '0')
    } else {
        amount
    }
    val digits = cents.trimStart('0').ifEmpty { "0" }
    return if (digits.length > MAX_CURRENCY_DIGITS) {
        Result.Error(IllegalArgumentException("Amount cannot exceed $9,999,999.99."))
    } else {
        Result.Success(digits)
    }
}

private fun changedText(previousText: String, newText: String): String {
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
    return newText.substring(prefixLength, insertedEnd)
}
