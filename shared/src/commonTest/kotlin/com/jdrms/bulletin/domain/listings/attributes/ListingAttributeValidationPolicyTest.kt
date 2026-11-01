package com.jdrms.bulletin.domain.listings.attributes

import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeValue
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributes
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeValidationPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ListingAttributeValidationPolicyTest {
    private val policy = ListingAttributeValidationPolicy()

    @Test
    fun emptyAttributesAreValidForEveryCategory() {
        ListingCategory.entries.forEach { assertTrue(policy.validate(it, ListingAttributes.EMPTY).isEmpty()) }
    }

    @Test
    fun suppliedValidAttributesPass() {
        assertTrue(policy.validate(ListingCategory.ELECTRONICS, laptopAttributes).isEmpty())
        assertTrue(policy.validate(ListingCategory.HOUSING, housingAttributes).isEmpty())
    }

    @Test
    fun capacitiesRequireASuitableDeviceType() {
        val monitor = laptopAttributes.with("device_type", ListingAttributeValue.Choice("monitor"))
        val keys = policy.validate(ListingCategory.ELECTRONICS, monitor).map { it.key }.toSet()
        assertEquals(setOf("ram_gb", "storage_gb"), keys)
        val noDevice = ListingAttributes(mapOf("ram_gb" to ListingAttributeValue.Integer(8)))
        assertEquals(listOf("ram_gb"), policy.validate(ListingCategory.ELECTRONICS, noDevice).map { it.key })
        val console = ListingAttributes(
            mapOf(
                "device_type" to ListingAttributeValue.Choice("console"),
                "storage_gb" to ListingAttributeValue.Integer(825)
            )
        )
        assertTrue(policy.validate(ListingCategory.ELECTRONICS, console).isEmpty())
    }

    @Test
    fun numericBoundariesAreInclusive() {
        fun ram(value: Long) = ListingAttributes(
            mapOf(
                "device_type" to ListingAttributeValue.Choice("laptop"),
                "ram_gb" to ListingAttributeValue.Integer(value)
            )
        )
        assertTrue(policy.validate(ListingCategory.ELECTRONICS, ram(1)).isEmpty())
        assertTrue(policy.validate(ListingCategory.ELECTRONICS, ram(4096)).isEmpty())
        assertEquals(1, policy.validate(ListingCategory.ELECTRONICS, ram(0)).size)
        assertEquals(1, policy.validate(ListingCategory.ELECTRONICS, ram(4097)).size)
    }

    @Test
    fun keysFromAnotherCategoryAreRejected() {
        val issues = policy.validate(ListingCategory.CLOTHING, laptopAttributes)
        assertTrue(issues.any { it.key == "ram_gb" })
        assertTrue(policy.validate(ListingCategory.OTHER, laptopAttributes).isNotEmpty())
    }

    @Test
    fun wrongValueKindIsRejected() {
        val wrong = ListingAttributes(mapOf("device_type" to ListingAttributeValue.Text("laptop")))
        assertEquals(listOf("device_type"), policy.validate(ListingCategory.ELECTRONICS, wrong).map { it.key })
    }

    @Test
    fun countAndPayloadSizeAreBounded() {
        val preserved = (1..20).associate { "extra_$it" to "\"v\"" }
        val issues = policy.validate(ListingCategory.OTHER, ListingAttributes(preserved = preserved))
        assertTrue(issues.any { it.key == null })
        val huge = ListingAttributes(preserved = mapOf("big" to "\"" + "x".repeat(3000) + "\""))
        assertTrue(policy.validate(ListingCategory.OTHER, huge).any { it.key == null })
    }
}
