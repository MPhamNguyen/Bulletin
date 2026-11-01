package com.jdrms.bulletin.domain.listings.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ListingIsbnInputTest {
    @Test
    fun formatterRemovesSeparatorsUppercasesXAndLimitsLength() {
        assertEquals("9780134685991", formatIsbnInput("978-0-13-468599-1"))
        assertEquals("080442957X", formatIsbnInput("0 8044 2957 x"))
        assertEquals("08044295", formatIsbnInput("0804X4295"))
        assertEquals("9780134685991", formatIsbnInput("97801346859912345"))
    }

    @Test
    fun checkerUsesTheRequestedShapesWithoutCheckingChecksums() {
        assertNull(isbnInputError("9780306406158"))
        assertNull(isbnInputError("030640615X"))
        assertNull(isbnInputError("9791234567890"))
        assertNull(isbnInputError("1234567890"))
        assertEqualsFormatError("12345678X0")
        assertNull(isbnInputError("123456789"))
    }

    private fun assertEqualsFormatError(value: String) {
        kotlin.test.assertNotNull(isbnInputError(value))
    }
}
