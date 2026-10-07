package com.jdrms.bulletin.domain.listings.presentation

import kotlin.math.round

private const val MAX_CURRENCY_DIGITS = 15

/** Keeps only cents digits so a currency field can behave like a cash-register keypad. */
internal fun normalizeCurrencyDigits(input: String): String {
    val digits = input.filter(Char::isDigit).take(MAX_CURRENCY_DIGITS)
    return if (digits.isEmpty()) "" else digits.trimStart('0').ifEmpty { "0" }
}

internal fun formatCurrencyDigits(digits: String): String {
    val cents = normalizeCurrencyDigits(digits).toLongOrNull() ?: 0L
    val dollars = cents / 100
    val remainder = (cents % 100).toString().padStart(2, '0')
    return "$$dollars.$remainder"
}

internal fun currencyDigitsToAmount(digits: String): Double =
    (normalizeCurrencyDigits(digits).toLongOrNull() ?: 0L) / 100.0

internal fun amountToCurrencyDigits(amount: Double): String =
    round(amount * 100).toLong().toString()
