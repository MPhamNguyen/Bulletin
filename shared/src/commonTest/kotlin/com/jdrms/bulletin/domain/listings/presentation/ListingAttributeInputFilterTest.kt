package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeCatalog
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeKeys
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import kotlin.test.Test
import kotlin.test.assertEquals

class ListingAttributeInputFilterTest {
    private fun filter(category: ListingCategory, key: String, raw: String) =
        ListingAttributeCatalog.definitionsFor(category).first { it.key == key }.filterInput(raw)

    private fun ram(raw: String) = filter(ListingCategory.ELECTRONICS, ListingAttributeKeys.RAM_GB, raw)
    private fun width(raw: String) = filter(ListingCategory.FURNITURE, ListingAttributeKeys.WIDTH_CM, raw)
    private fun isbn(raw: String) = filter(ListingCategory.TEXTBOOKS, ListingAttributeKeys.ISBN, raw)

    @Test
    fun wholeNumbersKeepOnlyDigitsWithinTheMaximumLength() {
        assertEquals("16", ram("16 GB"))
        assertEquals("16", ram("1.6-"))
        assertEquals("4096", ram("40960"))
        assertEquals("", ram("abc"))
    }

    @Test
    fun editionIsLimitedToTwoDigits() {
        assertEquals("12", filter(ListingCategory.TEXTBOOKS, ListingAttributeKeys.EDITION, "123"))
    }

    @Test
    fun decimalsAllowOnePointAndTheDefinedFractionDigits() {
        assertEquals("12.5", width("12.55"))
        assertEquals("12.5", width("12,5"))
        assertEquals("1.2", width("1.2.3"))
        assertEquals("12.5", width("12.5cm"))
        assertEquals(".5", width(".5"))
    }

    @Test
    fun rentAllowsTwoFractionDigits() {
        assertEquals("1250.55", filter(ListingCategory.HOUSING, ListingAttributeKeys.MONTHLY_RENT_USD, "1250.559"))
    }

    @Test
    fun isbnAllowsDigitsSeparatorsAndATrailingCheckX() {
        assertEquals("978-0-13-468599-1", isbn("978-0-13-468599-1"))
        assertEquals("080442957X", isbn("080442957x"))
        assertEquals("08044295", isbn("0804X4295"))
        assertEquals("9780134685991", isbn("97801346859912345"))
        assertEquals("123", isbn("1a2b3"))
    }

    @Test
    fun courseCodeAndSizeRejectOtherCharacters() {
        assertEquals("CECS 328", filter(ListingCategory.TEXTBOOKS, ListingAttributeKeys.COURSE_CODE, "CECS 328!"))
        assertEquals("XL", filter(ListingCategory.CLOTHING, ListingAttributeKeys.SIZE, "xl$"))
        assertEquals("10.5/W", filter(ListingCategory.CLOTHING, ListingAttributeKeys.SIZE, "10.5/w"))
    }

    @Test
    fun brandIsLimitedToItsMaximumLength() {
        assertEquals(40, filter(ListingCategory.ELECTRONICS, ListingAttributeKeys.BRAND, "a".repeat(60)).length)
    }
}
