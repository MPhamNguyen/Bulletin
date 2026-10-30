package com.jdrms.bulletin.domain.listings.attributes

import com.jdrms.bulletin.domain.listings.domain.model.ListingDecimal
import com.jdrms.bulletin.domain.listings.domain.model.ListingTextFormat
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class ListingAttributeTextTest {
    @Test
    fun brandMatchKeyIgnoresCaseAndPunctuationButNotLetters() {
        assertEquals("levis", ListingAttributeText.matchKey("  Levi's "))
        assertEquals("levis", ListingAttributeText.matchKey("LEVI’S"))
        assertEquals("under armour", ListingAttributeText.matchKey("Under-Armour"))
        assertEquals("h and m", ListingAttributeText.matchKey("H&M"))
        assertNotEquals(ListingAttributeText.matchKey("Dell"), ListingAttributeText.matchKey("Dell Inc"))
        assertNotEquals(ListingAttributeText.matchKey("Pen Tel"), ListingAttributeText.matchKey("Pentel"))
    }

    @Test
    fun brandKeepsDisplaySpellingWhenNormalizedForStorage() {
        assertEquals("Levi's Strauss", ListingAttributeText.normalize(ListingTextFormat.BRAND, " Levi's   Strauss "))
        assertNull(ListingAttributeText.normalize(ListingTextFormat.BRAND, "---"))
        assertNull(ListingAttributeText.normalize(ListingTextFormat.BRAND, "Bad\nBrand"))
    }

    @Test
    fun isbnIsStoredAsTextWithLeadingZerosAndChecksumValidated() {
        assertEquals("0306406152", ListingAttributeText.normalize(ListingTextFormat.ISBN, "0-306-40615-2"))
        assertEquals("9780306406157", ListingAttributeText.normalize(ListingTextFormat.ISBN, "978 0 306 40615 7"))
        assertEquals("080442957X", ListingAttributeText.normalize(ListingTextFormat.ISBN, "0-8044-2957-x"))
        assertNull(ListingAttributeText.normalize(ListingTextFormat.ISBN, "9780306406158"))
        assertNull(ListingAttributeText.normalize(ListingTextFormat.ISBN, "12345"))
    }

    @Test
    fun courseCodeHasOneCanonicalSpelling() {
        assertEquals("CECS 328", ListingAttributeText.normalize(ListingTextFormat.COURSE_CODE, "cecs328"))
        assertEquals("CECS 328", ListingAttributeText.normalize(ListingTextFormat.COURSE_CODE, "CECS-328"))
        assertEquals("MATH 122A", ListingAttributeText.normalize(ListingTextFormat.COURSE_CODE, "math 122a"))
        assertNull(ListingAttributeText.normalize(ListingTextFormat.COURSE_CODE, "328"))
    }

    @Test
    fun clothingSizeIsUppercasedAndRestricted() {
        assertEquals("XL", ListingAttributeText.normalize(ListingTextFormat.CLOTHING_SIZE, "xl"))
        assertEquals("32/30", ListingAttributeText.normalize(ListingTextFormat.CLOTHING_SIZE, "32/30"))
        assertNull(ListingAttributeText.normalize(ListingTextFormat.CLOTHING_SIZE, "<script>"))
        assertNull(ListingAttributeText.normalize(ListingTextFormat.CLOTHING_SIZE, "1234567890123"))
    }

    @Test
    fun decimalIsExactAndRejectsExcessPrecision() {
        assertEquals("1250.50", ListingDecimal.parse("1250.5", 2)?.toPlainString())
        assertEquals(125050L, ListingDecimal.parse("1250.50", 2)?.unscaled)
        assertEquals("0.07", ListingDecimal.parse("0.07", 2)?.toPlainString())
        assertEquals(1250L, ListingDecimal.parse("12.500", 2)?.unscaled)
        assertNull(ListingDecimal.parse("12.505", 2))
        assertNull(ListingDecimal.parse("1e3", 2))
        assertNull(ListingDecimal.parse("-5", 2))
        assertNull(ListingDecimal.parse("1,250", 2))
    }
}
