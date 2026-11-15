package com.jdrms.bulletin.domain.marketplace.domain.model

import kotlin.math.roundToLong

data class MarketplacePriceRange(
    val minimumCents: Long? = null,
    val maximumCents: Long? = null
) {
    init {
        require(minimumCents == null || minimumCents >= 0L) { "Minimum price cannot be negative." }
        require(maximumCents == null || maximumCents >= 0L) { "Maximum price cannot be negative." }
        require(minimumCents == null || maximumCents == null || minimumCents <= maximumCents) {
            "Minimum price cannot exceed maximum price."
        }
    }

    val isEmpty: Boolean
        get() = minimumCents == null && maximumCents == null

    fun contains(amount: Double): Boolean {
        val cents = (amount * CENTS_PER_DOLLAR).roundToLong()
        return (minimumCents == null || cents >= minimumCents) &&
            (maximumCents == null || cents <= maximumCents)
    }

    val minimumAmount: Double?
        get() = minimumCents?.div(CENTS_PER_DOLLAR)

    val maximumAmount: Double?
        get() = maximumCents?.div(CENTS_PER_DOLLAR)

    companion object {
        val NONE = MarketplacePriceRange()

        private const val CENTS_PER_DOLLAR = 100.0
    }
}
