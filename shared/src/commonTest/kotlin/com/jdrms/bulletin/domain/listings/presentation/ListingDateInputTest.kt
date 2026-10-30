package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeCatalog
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeDate
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeKeys
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeInputParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ListingDateInputTest {
    private val parser = ListingAttributeInputParser()
    private val definition = ListingAttributeCatalog.definitionsFor(ListingCategory.HOUSING)
        .first { it.key == ListingAttributeKeys.AVAILABLE_DATE }

    private fun error(text: String) = dateInputError(parser, ListingCategory.HOUSING, definition, text)

    @Test
    fun eightDigitsFormatAsAnIsoDate() {
        assertEquals("2026-08-16", formatDateDigits(dateDigits("20260816")))
    }

    @Test
    fun typedIsoDateIsKeptAsIs() {
        assertEquals("2026-08-16", formatDateDigits(dateDigits("2026-08-16")))
    }

    @Test
    fun digitsStayInPlaceAndExtrasAreDropped() {
        assertEquals("2026-0", formatDateDigits(dateDigits("2026-0")))
        assertEquals("2026", formatDateDigits(dateDigits("2026-")))
        assertEquals("2026-08-16", formatDateDigits(dateDigits("20260816999")))
        assertEquals("2026-08", formatDateDigits(dateDigits("2a0b2c6d0e8")))
    }

    @Test
    fun cursorSkipsOverInsertedHyphens() {
        assertEquals(4, formattedCursorOffset(4))
        assertEquals(6, formattedCursorOffset(5))
        assertEquals(10, formattedCursorOffset(8))
    }

    @Test
    fun realDatesAreAcceptedAndImpossibleOnesExplained() {
        assertNull(error("2026-08-16"))
        assertEquals(ListingAttributeDate(2026, 8, 16), parseDateInput("2026-08-16"))
        assertEquals("That date doesn't exist. Check the month and day.", error("2026-02-30"))
        assertEquals("That date doesn't exist. Check the month and day.", error("2026-13-01"))
        assertNull(parseDateInput("2026-02-30"))
    }

    @Test
    fun yearsOutsideTheAllowedRangeAreExplained() {
        assertNotNull(error("1999-01-01"))
    }

    @Test
    fun incompleteEntriesAreNotYetErrors() {
        assertNull(error("2026-08"))
        assertNull(error(""))
    }
}
