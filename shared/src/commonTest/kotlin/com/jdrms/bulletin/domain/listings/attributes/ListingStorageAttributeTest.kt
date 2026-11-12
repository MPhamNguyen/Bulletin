package com.jdrms.bulletin.domain.listings.attributes

import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeKeys
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeValue
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingStorageOptions
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeInputParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ListingStorageAttributeTest {
    private val parser = ListingAttributeInputParser()
    private val key = ListingAttributeKeys.STORAGE_GB

    private fun parse(storage: String) = parser.parse(
        ListingCategory.ELECTRONICS,
        mapOf(ListingAttributeKeys.DEVICE_TYPE to "laptop", key to storage)
    )

    @Test
    fun everyPredefinedStorageSizeIsAccepted() {
        ListingStorageOptions.gigabytes.forEach { size ->
            assertEquals(ListingAttributeValue.Integer(size), parse(size.toString()).attributes?.get(key))
        }
    }

    @Test
    fun sizesOutsideThePredefinedSetAreRejected() {
        val result = parse("500")

        assertNull(result.attributes)
        assertEquals("Choose a valid storage size.", result.errors[key])
    }

    @Test
    fun aSecondStorageSizeInTheSameInputIsRejected() {
        val result = parse("256,512")

        assertNull(result.attributes)
        assertEquals("Storage must be a whole number.", result.errors[key])
    }

    @Test
    fun labelsUseTerabytesFromOneTerabyte() {
        assertEquals(
            listOf("128 GB", "256 GB", "512 GB", "1 TB", "2 TB"),
            ListingStorageOptions.gigabytes.map(ListingStorageOptions::label)
        )
    }
}
