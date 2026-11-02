package com.jdrms.bulletin.core.designsystem

import kotlin.math.round

internal const val MAX_CURRENCY_DIGITS = 9

/**
 * Keeps only ASCII cents digits so a currency field can behave like a cash-register keypad.
 * Empty input represents an unset price; an explicit zero remains a valid price.
 */
internal fun normalizeCurrencyDigits(input: String): String {
    val digits = input.filter { it in '0'..'9' }
    return digits.trimStart('0').take(MAX_CURRENCY_DIGITS).ifEmpty {
        if (digits.isEmpty()) "" else "0"
    }
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

/** Converts a valid, non-negative amount to cents digits. */
internal fun amountToCurrencyDigits(amount: Double): String {
    require(amount.isFinite() && amount >= 0.0) { "Currency amount must be finite and non-negative." }
    val cents = round(amount * 100).toLong()
    require(cents <= 999_999_999L) { "Currency amount exceeds the listing price limit." }
    return cents.toString()
}
