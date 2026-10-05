package com.jdrms.bulletin.core.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CurrencyFormattingTest {

    @Test
    fun formatsZeroWithTwoDecimalPlaces() {
        assertEquals("$0.00", formatUsdAmount(0.0))
        assertTrue(hasAtMostTwoDecimalPlaces(0.0))
    }

    @Test
    fun roundsAtCentBoundaries() {
        assertEquals("$1.00", formatUsdAmount(1.004))
        assertEquals("$1.01", formatUsdAmount(1.005))
        assertEquals("$1.01", formatUsdAmount(1.006))
    }

    @Test
    fun rejectsNonFiniteAmounts() {
        assertFalse(hasAtMostTwoDecimalPlaces(Double.NaN))
        assertFalse(hasAtMostTwoDecimalPlaces(Double.POSITIVE_INFINITY))
        assertFalse(hasAtMostTwoDecimalPlaces(Double.NEGATIVE_INFINITY))

        assertFailsWith<IllegalArgumentException> { formatUsdAmount(Double.NaN) }
        assertFailsWith<IllegalArgumentException> { formatUsdAmount(Double.POSITIVE_INFINITY) }
        assertFailsWith<IllegalArgumentException> { formatUsdAmount(Double.NEGATIVE_INFINITY) }
    }

    @Test
    fun rejectsAmountsTooLargeToRepresentAsCents() {
        assertFalse(hasAtMostTwoDecimalPlaces(Double.MAX_VALUE))
        assertFailsWith<IllegalArgumentException> { formatUsdAmount(Double.MAX_VALUE) }
    }
}
