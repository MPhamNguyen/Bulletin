package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals

class CurrencyInputTest {

    @Test
    fun digitsEnterFromTheHundredthsPlace() {
        var digits = ""

        digits = normalizeCurrencyTextInput(formatCurrencyDigits(digits) + "1", formatCurrencyDigits(digits))
        assertEquals("$0.01", formatCurrencyDigits(digits))
        digits = normalizeCurrencyTextInput(formatCurrencyDigits(digits) + "2", formatCurrencyDigits(digits))
        assertEquals("$0.12", formatCurrencyDigits(digits))
        digits = normalizeCurrencyTextInput(formatCurrencyDigits(digits) + "5", formatCurrencyDigits(digits))
        assertEquals("$1.25", formatCurrencyDigits(digits))
        digits = normalizeCurrencyTextInput(formatCurrencyDigits(digits) + "0", formatCurrencyDigits(digits))
        assertEquals("$12.50", formatCurrencyDigits(digits))
    }

    @Test
    fun deletingTheLastDigitMovesTheDecimalLeft() {
        val displayed = formatCurrencyDigits("125")
        assertEquals("12", normalizeCurrencyTextInput(displayed.dropLast(1), displayed))
        assertEquals("$0.12", formatCurrencyDigits("12"))
        assertEquals("", normalizeCurrencyTextInput("$0.0", "$0.00"))
    }

    @Test
    fun pastedCurrencyTextUsesOnlyItsDigits() {
        assertEquals("1250", normalizeCurrencyTextInput("$12.50", ""))
        assertEquals("1250", normalizeCurrencyTextInput("12.5", ""))
        assertEquals("12", normalizeCurrencyTextInput("12", ""))
        assertEquals(12.5, currencyDigitsToAmount("1250"))
    }

    @Test
    fun pasteAtTheEndUsesOnlyThePastedPayload() {
        val previous = TextFieldValue("$1.25", selection = TextRange(5))
        assertEquals(
            "1250",
            normalizeCurrencyTextChange(
                TextFieldValue("$1.2512.5", selection = TextRange(9)),
                previous
            )
        )
    }

    @Test
    fun emptyAndZeroAreUnset() {
        assertEquals("", normalizeCurrencyDigits("0"))
        assertEquals("", normalizeCurrencyDigits("0000"))
        assertEquals("", formatCurrencyDigits(""))
        assertEquals("", formatCurrencyDigits("0"))
    }

    @Test
    fun normalizationTrimsLeadingZerosBeforeApplyingTheCap() {
        assertEquals("1250", normalizeCurrencyDigits("0000000000000012.50"))
        assertEquals(MAX_CURRENCY_DIGITS, normalizeCurrencyDigits("1234567890123").length)
        assertEquals("", normalizeCurrencyDigits("-٥abc"))
    }

    @Test
    fun amountConversionRoundsFloatingPointCents() {
        assertEquals("1999", amountToCurrencyDigits(19.99))
        assertEquals("5", amountToCurrencyDigits(0.05))
        assertEquals("", amountToCurrencyDigits(0.0))
    }

    @Test
    fun formattingAddsThousandsSeparators() {
        assertEquals("$1,234,567.89", formatCurrencyDigits("123456789"))
    }

    @Test
    fun typingAfterMovingTheCursorAppendsAtTheEnd() {
        val previous = TextFieldValue("$1.25", selection = TextRange(2))
        assertEquals(
            "1259",
            normalizeCurrencyTextChange(TextFieldValue("$19.25", selection = TextRange(3)), previous)
        )
    }

    @Test
    fun deletingTheDecimalPointBehavesLikeEndBackspace() {
        val previous = TextFieldValue("$1.25", selection = TextRange(3))
        assertEquals(
            "12",
            normalizeCurrencyTextChange(TextFieldValue("$125", selection = TextRange(2)), previous)
        )
    }

    @Test
    fun deletingASelectedMiddleRangeBehavesLikeEndBackspace() {
        val previous = TextFieldValue("$12.34", selection = TextRange(2, 5))
        assertEquals(
            "123",
            normalizeCurrencyTextChange(TextFieldValue("$14", selection = TextRange(2)), previous)
        )
    }

    @Test
    fun replacingAllTextUsesTheReplacementAsAStandaloneAmount() {
        val previous = TextFieldValue("$1.25", selection = TextRange(0, 5))
        assertEquals(
            "1250",
            normalizeCurrencyTextChange(TextFieldValue("12.5", selection = TextRange(4)), previous)
        )
    }
}
