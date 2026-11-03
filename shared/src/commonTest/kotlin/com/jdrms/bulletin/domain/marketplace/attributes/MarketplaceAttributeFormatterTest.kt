package com.jdrms.bulletin.domain.marketplace.attributes

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeValue
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributes
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.service.MarketplaceAttributeFormatter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarketplaceAttributeFormatterTest {
    @Test
    fun showsLabelsAndUnitsInCatalogOrder() {
        val lines = MarketplaceAttributeFormatter.linesFor(
            MarketplaceCategory.ELECTRONICS,
            MarketplaceAttributes(
                mapOf(
                    "storage_gb" to MarketplaceAttributeValue.Integer(512),
                    "device_type" to MarketplaceAttributeValue.Choice("laptop"),
                    "brand" to MarketplaceAttributeValue.Text("Framework"),
                    "ram_gb" to MarketplaceAttributeValue.Integer(16)
                )
            )
        ).map { it.label to it.value }
        assertEquals(
            listOf("Brand" to "Framework", "Device type" to "Laptop", "RAM" to "16 GB", "Storage" to "512 GB"),
            lines
        )
    }

    @Test
    fun rentIsLabelledAsMonthlyMoneyAndDatesAreReadable() {
        val lines = MarketplaceAttributeFormatter.linesFor(
            MarketplaceCategory.HOUSING,
            MarketplaceAttributes(
                mapOf(
                    "monthly_rent_usd" to MarketplaceAttributeValue.Decimal("1250.50"),
                    "available_date" to MarketplaceAttributeValue.Date("2026-09-01")
                )
            )
        ).map { it.label to it.value }
        assertEquals(listOf("Monthly rent" to "\$1,250.50/mo", "Available date" to "Sep 1, 2026"), lines)
    }

    @Test
    fun emptyUnknownOrWrongCategoryAttributesRenderNothing() {
        assertTrue(
            MarketplaceAttributeFormatter.linesFor(
                MarketplaceCategory.ELECTRONICS,
                MarketplaceAttributes.EMPTY
            ).isEmpty()
        )
        val stray = MarketplaceAttributes(mapOf("future_key" to MarketplaceAttributeValue.Text("x")))
        assertTrue(MarketplaceAttributeFormatter.linesFor(MarketplaceCategory.ELECTRONICS, stray).isEmpty())
        val ram = MarketplaceAttributes(mapOf("ram_gb" to MarketplaceAttributeValue.Integer(8)))
        assertTrue(MarketplaceAttributeFormatter.linesFor(MarketplaceCategory.CLOTHING, ram).isEmpty())
    }
}
