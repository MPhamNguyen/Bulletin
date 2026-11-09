package com.jdrms.bulletin.domain.listings.attributes

import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeCatalog
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeKeys
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeType
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeValue
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeInputParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ListingClothingColorAttributeTest {
    private val parser = ListingAttributeInputParser()

    @Test
    fun clothingOffersTheSupportedColorChoices() {
        val definition = ListingAttributeCatalog.definition(ListingCategory.CLOTHING, ListingAttributeKeys.COLOR)
        val type = definition?.type as? ListingAttributeType.Choice

        assertEquals(
            listOf(
                "black", "white", "gray", "red", "orange", "yellow", "green", "blue", "purple", "pink",
                "brown", "beige", "multicolor", "other"
            ),
            type?.options?.map { it.code }
        )
    }

    @Test
    fun clothingColorParsesAsAChoiceAndRejectsUnknownValues() {
        val parsed = parser.parse(ListingCategory.CLOTHING, mapOf(ListingAttributeKeys.COLOR to "blue"))
        val invalid = parser.parse(ListingCategory.CLOTHING, mapOf(ListingAttributeKeys.COLOR to "teal"))

        assertEquals(ListingAttributeValue.Choice("blue"), parsed.attributes?.get(ListingAttributeKeys.COLOR))
        assertNull(invalid.attributes)
        assertEquals("Choose a valid color.", invalid.errors[ListingAttributeKeys.COLOR])
    }
}
