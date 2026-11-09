package com.jdrms.bulletin.domain.listings.attributes

import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeKeys
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeValue
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeInputParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ListingClothingSizeAttributeTest {
    private val parser = ListingAttributeInputParser()

    @Test
    fun sizeChoicesDependOnClothingType() {
        assertEquals(setOf("clothing_type", "color", "brand"), visibleKeys(emptyMap()))
        assertTrue("size" in visibleKeys(mapOf("clothing_type" to "top")))
        assertEquals(
            setOf("clothing_type", "shoe_size_system", "color", "brand"),
            visibleKeys(mapOf("clothing_type" to "shoes"))
        )
        assertTrue(
            "size" in visibleKeys(mapOf("clothing_type" to "shoes", "shoe_size_system" to "us_men"))
        )
    }

    @Test
    fun structuredSizesRejectRandomOrIncompatibleValues() {
        val top = parser.parse(ListingCategory.CLOTHING, mapOf("clothing_type" to "top", "size" to "M"))
        val random = parser.parse(ListingCategory.CLOTHING, mapOf("clothing_type" to "top", "size" to "BANANA"))
        val wrongKind = parser.parse(ListingCategory.CLOTHING, mapOf("clothing_type" to "top", "size" to "32"))
        val bottom = parser.parse(ListingCategory.CLOTHING, mapOf("clothing_type" to "bottom", "size" to "32"))

        assertEquals(ListingAttributeValue.Choice("M"), top.attributes?.get(ListingAttributeKeys.SIZE))
        assertNull(random.attributes)
        assertEquals("Choose a valid size.", random.errors[ListingAttributeKeys.SIZE])
        assertNull(wrongKind.attributes)
        assertEquals(ListingAttributeValue.Choice("32"), bottom.attributes?.get(ListingAttributeKeys.SIZE))
    }

    @Test
    fun shoeSizesRequireADeclaredSystem() {
        val us = parser.parse(
            ListingCategory.CLOTHING,
            mapOf("clothing_type" to "shoes", "shoe_size_system" to "us_men", "size" to "10.5")
        )
        val eu = parser.parse(
            ListingCategory.CLOTHING,
            mapOf("clothing_type" to "shoes", "shoe_size_system" to "eu", "size" to "42")
        )
        val incompatible = parser.parse(
            ListingCategory.CLOTHING,
            mapOf("clothing_type" to "shoes", "shoe_size_system" to "eu", "size" to "10.5")
        )

        assertEquals(ListingAttributeValue.Choice("10.5"), us.attributes?.get(ListingAttributeKeys.SIZE))
        assertEquals(ListingAttributeValue.Choice("42"), eu.attributes?.get(ListingAttributeKeys.SIZE))
        assertNull(incompatible.attributes)
    }

    private fun visibleKeys(inputs: Map<String, String>): Set<String> =
        parser.applicableDefinitions(ListingCategory.CLOTHING, inputs).map { it.key }.toSet()
}
