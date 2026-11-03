package com.jdrms.bulletin.core.designsystem

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.jdrms.bulletin.core.common.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CurrencyTextInputTest {
    @Test
    fun negativeAmountsReturnAnErrorFromAllPastePaths() {
        listOf("-1.00", "-$1.00", "$-1.00", "-100").forEach { input ->
            assertRejectedPaste(input, "Amount cannot be negative.")
        }
    }

    @Test
    fun oversizedAmountsReturnAnErrorInsteadOfThrowingOrTruncating() {
        listOf("10000000.00", "$10,000,000.00", "9999999999", "9".repeat(400) + ".00").forEach { input ->
            assertRejectedPaste(input, "Amount cannot exceed $9,999,999.99.")
        }
    }

    @Test
    fun malformedAndNonFiniteAmountsReturnAnError() {
        listOf("NaN", "Infinity", "1.0e309", "1.2.3", "1.001", "abc").forEach { input ->
            assertRejectedPaste(input, "Enter a valid amount with up to two decimal places.")
        }
    }

    @Test
    fun typingPastTheDigitLimitReturnsAnError() {
        val previous = formatCurrencyDigits("999999999")
        val result = normalizeCurrencyTextInput(previous + "9", previous)
        assertEquals("Amount cannot exceed $9,999,999.99.", assertIs<Result.Error>(result).message)
    }

    @Test
    fun boundaryAmountsAndLeadingZerosRemainValid() {
        assertEquals(Result.Success("999999999"), normalizeCurrencyReplacement("$9,999,999.99"))
        assertEquals(Result.Success("999999999"), normalizeCurrencyReplacement("999999999"))
        assertEquals(Result.Success("1250"), normalizeCurrencyReplacement("000000000000012.50"))
        assertEquals(Result.Success("50"), normalizeCurrencyReplacement(".5"))
        assertEquals(Result.Success("0"), normalizeCurrencyReplacement("0.00"))
        assertEquals(Result.Success(""), normalizeCurrencyReplacement(""))
    }

    @Test
    fun replacingAReversedFullSelectionParsesTheEntireReplacement() {
        val previous = TextFieldValue("$1.25", selection = TextRange(5, 0))
        assertEquals(Result.Success("0"), normalizeCurrencyTextChange(TextFieldValue("0.00"), previous))
        assertIs<Result.Error>(normalizeCurrencyTextChange(TextFieldValue("-1.00"), previous))
    }

    @Test
    fun aShorterNegativePasteOverAPartialSelectionReturnsAnError() {
        val previous = TextFieldValue("$123,456.78", selection = TextRange(1, 10))
        val result = normalizeCurrencyTextChange(TextFieldValue("$-1.00"), previous)
        assertEquals("Amount cannot be negative.", assertIs<Result.Error>(result).message)
    }

    @Test
    fun insertingDigitsPreservesLeadingZerosInThePayload() {
        assertEquals(Result.Success("1250001"), normalizeCurrencyTextInput("$1.250001", "$1.25"))
    }

    private fun assertRejectedPaste(input: String, message: String) {
        val previousText = "$1.25"
        val previous = TextFieldValue(previousText, selection = TextRange(previousText.length))
        val appended = TextFieldValue(previousText + input)
        val selected = previous.copy(selection = TextRange(0, previousText.length))
        val results = listOf(
            normalizeCurrencyReplacement(input),
            normalizeCurrencyTextInput(input, ""),
            normalizeCurrencyTextChange(appended, previous),
            normalizeCurrencyTextChange(TextFieldValue(input), selected)
        )
        results.forEach { assertEquals(message, assertIs<Result.Error>(it).message, input) }
    }
}
