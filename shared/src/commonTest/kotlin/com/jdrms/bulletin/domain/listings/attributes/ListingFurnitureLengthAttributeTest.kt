package com.jdrms.bulletin.domain.listings.attributes

import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeKeys
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeValue
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingDecimal
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeInputParser
import kotlin.test.Test
import kotlin.test.assertEquals

class ListingFurnitureLengthAttributeTest {
    @Test
    fun furnitureLengthParsesAsCentimetersWithOneDecimalPlace() {
        val parsed = ListingAttributeInputParser().parse(
            ListingCategory.FURNITURE,
            mapOf(ListingAttributeKeys.LENGTH_CM to "125.5")
        )

        assertEquals(
            ListingAttributeValue.Decimal(ListingDecimal(1_255L, 1)),
            parsed.attributes?.get(ListingAttributeKeys.LENGTH_CM)
        )
    }
}
