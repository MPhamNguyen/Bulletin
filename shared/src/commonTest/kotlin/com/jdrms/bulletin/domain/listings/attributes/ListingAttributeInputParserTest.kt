package com.jdrms.bulletin.domain.listings.attributes

import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeValue
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeInputFormatter
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeInputParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ListingAttributeInputParserTest {
    private val parser = ListingAttributeInputParser()

    @Test
    fun blankOptionalFieldsAreOmitted() {
        val result = parser.parse(ListingCategory.ELECTRONICS, mapOf("brand" to "  ", "device_type" to ""))
        assertEquals(emptyMap(), result.attributes?.values)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun parsesTypedValuesAndRoundTripsThroughTheFormatter() {
        val inputs = mapOf("monthly_rent_usd" to "1250.5", "available_date" to "2026-09-01", "housing_type" to "room")
        val attributes = parser.parse(ListingCategory.HOUSING, inputs).attributes
        assertNotNull(attributes)
        assertEquals("1250.50", ListingAttributeInputFormatter.inputFor(attributes.values.getValue("monthly_rent_usd")))
        assertEquals(housingAttributes, attributes)
        assertEquals(ListingAttributeValue.Choice("room"), attributes["housing_type"])
    }

    @Test
    fun invalidInputKeepsPerFieldErrorsAndProducesNoAttributes() {
        val result = parser.parse(
            ListingCategory.ELECTRONICS,
            mapOf("device_type" to "laptop", "ram_gb" to "sixteen", "storage_gb" to "99999999")
        )
        assertNull(result.attributes)
        assertEquals(setOf("ram_gb", "storage_gb"), result.errors.keys)
    }

    @Test
    fun hiddenDependentFieldIsIgnoredRatherThanSubmitted() {
        val result = parser.parse(ListingCategory.ELECTRONICS, mapOf("device_type" to "monitor", "ram_gb" to "16"))
        assertEquals(setOf("device_type"), result.attributes?.values?.keys)
    }

    @Test
    fun preservedValuesAreCarriedUnlessReplacedByTheSeller() {
        val preserved = mapOf("future_key" to "\"kept\"", "brand" to "\"bad shape\"")
        val result = parser.parse(ListingCategory.ELECTRONICS, mapOf("brand" to "Dell"), preserved)
        assertEquals(mapOf("future_key" to "\"kept\""), result.attributes?.preserved)
    }

    @Test
    fun dateAndDecimalFormatsAreStrict() {
        assertTrue(parser.parse(ListingCategory.HOUSING, mapOf("available_date" to "2026-02-30")).errors.isNotEmpty())
        assertTrue(parser.parse(ListingCategory.HOUSING, mapOf("monthly_rent_usd" to "12.345")).errors.isNotEmpty())
        assertTrue(parser.parse(ListingCategory.HOUSING, mapOf("monthly_rent_usd" to "0")).errors.isNotEmpty())
        assertTrue(parser.parse(ListingCategory.FURNITURE, mapOf("width_cm" to "60.5")).errors.isEmpty())
    }
}
