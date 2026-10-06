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
    fun formatsThousandsWithSeparators() {
        assertEquals("$999.99", formatUsdAmount(999.99))
        assertEquals("$1,000.00", formatUsdAmount(1000.0))
        assertEquals("$1,234.56", formatUsdAmount(1234.56))
        assertEquals("$12,345.67", formatUsdAmount(12345.67))
        assertEquals("$123,456.78", formatUsdAmount(123456.78))
        assertEquals("$1,000,000.00", formatUsdAmount(1000000.0))
        assertEquals("$1,234,567,890.12", formatUsdAmount(1234567890.12))
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
