package com.jdrms.bulletin.core.common

import kotlin.math.abs
import kotlin.math.round

private const val CURRENCY_DECIMAL_TOLERANCE = 0.000000001
private const val CURRENCY_ROUNDING_EPSILON = 0.000000001
private const val MAX_CURRENCY_AMOUNT = Long.MAX_VALUE / 100.0

/** Returns whether an amount can be represented with no more than two decimal places. */
fun hasAtMostTwoDecimalPlaces(amount: Double): Boolean {
    if (!amount.isFinite() || amount > MAX_CURRENCY_AMOUNT) return false
    val roundedToCents = round(amount * 100.0) / 100.0
    return abs(amount - roundedToCents) < CURRENCY_DECIMAL_TOLERANCE
}

/** Formats a USD amount with a stable, locale-independent two-decimal presentation. */
fun formatUsdAmount(amount: Double): String {
    require(amount >= 0.0) { "Currency amount cannot be negative." }
    require(amount.isFinite()) { "Currency amount must be finite." }
    require(amount <= MAX_CURRENCY_AMOUNT) { "Currency amount is too large." }

    val roundedCents = round(amount * 100.0 + CURRENCY_ROUNDING_EPSILON).toLong()
    val dollars = roundedCents / 100
    val cents = (roundedCents % 100).toString().padStart(2, '0')
    return "$$dollars.$cents"
}
