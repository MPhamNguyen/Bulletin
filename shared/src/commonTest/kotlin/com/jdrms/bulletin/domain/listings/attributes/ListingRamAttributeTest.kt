package com.jdrms.bulletin.domain.listings.attributes

import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeKeys
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeValue
import com.jdrms.bulletin.domain.listings.domain.model.ListingCapacityOptions
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeInputParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ListingRamAttributeTest {
    private val parser = ListingAttributeInputParser()
    private val key = ListingAttributeKeys.RAM_GB

    private fun parse(ram: String) = parser.parse(
        ListingCategory.ELECTRONICS,
        mapOf(ListingAttributeKeys.DEVICE_TYPE to "laptop", key to ram)
    )

    @Test
    fun everyPredefinedRamSizeIsAccepted() {
        ListingCapacityOptions.ram.forEach { size ->
            assertEquals(ListingAttributeValue.Integer(size), parse(size.toString()).attributes?.get(key))
        }
    }

    @Test
    fun sizesOutsideThePredefinedSetAreRejected() {
        val result = parse("12")

        assertNull(result.attributes)
        assertEquals("Choose a valid ram size.", result.errors[key])
    }

    @Test
    fun aSecondRamSizeInTheSameInputIsRejected() {
        val result = parse("8,16")

        assertNull(result.attributes)
        assertEquals("RAM must be a whole number.", result.errors[key])
    }

    @Test
    fun optionsMatchTheMarketplaceRamChoices() {
        assertEquals(listOf(4L, 8L, 16L, 32L, 64L, 128L), ListingCapacityOptions.ram)
    }
}
