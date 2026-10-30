package com.jdrms.bulletin.domain.listings.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class ListingRentInputTest {
    @Test
    fun typingRentStartsWithCentsAndStoresDecimalAmounts() {
        var text = ""
        listOf("0.01", "0.12", "1.25", "12.50").zip("1250".toList()).forEach { (amount, digit) ->
            val digits = normalizeCurrencyTextInput(text + digit, text)
            assertEquals(amount, rentDigitsToAmount(digits))
            text = formatCurrencyDigits(digits)
        }
    }

    @Test
    fun existingRentUsesDollarsAndPreservesCents() {
        assertEquals("125050", rentAmountToDigits("1250.50"))
        assertEquals("125000", rentAmountToDigits("1250"))
        assertEquals("1250.50", rentDigitsToAmount(rentAmountToDigits("1250.50")))
    }

    @Test
    fun clearingRentKeepsTheOptionalAttributeUnset() {
        assertEquals("", rentAmountToDigits(""))
        assertEquals("", rentDigitsToAmount(""))
        assertEquals("0.00", rentDigitsToAmount("0"))
    }
}
