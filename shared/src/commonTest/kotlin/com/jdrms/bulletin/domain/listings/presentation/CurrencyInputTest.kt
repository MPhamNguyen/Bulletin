package com.jdrms.bulletin.domain.listings.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class CurrencyInputTest {

    @Test
    fun digitsEnterFromTheHundredthsPlace() {
        var digits = ""

        digits = normalizeCurrencyDigits(digits + "1")
        assertEquals("$0.01", formatCurrencyDigits(digits))
        digits = normalizeCurrencyDigits(digits + "2")
        assertEquals("$0.12", formatCurrencyDigits(digits))
        digits = normalizeCurrencyDigits(digits + "5")
        assertEquals("$1.25", formatCurrencyDigits(digits))
        digits = normalizeCurrencyDigits(digits + "0")
        assertEquals("$12.50", formatCurrencyDigits(digits))
    }

    @Test
    fun deletingTheLastDigitMovesTheDecimalLeft() {
        assertEquals("$1.25", formatCurrencyDigits("125"))
        assertEquals("$0.12", formatCurrencyDigits("12"))
        assertEquals("$0.01", formatCurrencyDigits("1"))
        assertEquals("$0.00", formatCurrencyDigits(""))
    }

    @Test
    fun pastedCurrencyTextUsesOnlyItsDigits() {
        assertEquals("1250", normalizeCurrencyDigits("$12.50"))
        assertEquals(12.5, currencyDigitsToAmount("$12.50"))
    }
}
