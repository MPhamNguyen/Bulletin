package com.jdrms.bulletin.domain.marketplace.attributes

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeValue
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributes
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.infrastructure.mapper.MarketplaceAttributesJsonDecoder
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class MarketplaceAttributesJsonDecoderTest {
    private fun decode(json: String?, category: MarketplaceCategory = MarketplaceCategory.ELECTRONICS) =
        MarketplaceAttributesJsonDecoder.decode(json?.let(Json::parseToJsonElement), category)

    @Test
    fun decodesTypedValuesForTheCategory() {
        val decoded = decode(
            """{"brand":"Framework","device_type":"laptop","ram_gb":16,"storage_gb":512,"future":"x"}"""
        )
        assertEquals(MarketplaceAttributeValue.Integer(16), decoded["ram_gb"])
        assertEquals(MarketplaceAttributeValue.Choice("laptop"), decoded["device_type"])
        assertEquals(setOf("brand", "device_type", "ram_gb", "storage_gb"), decoded.values.keys)
    }

    @Test
    fun keepsExactDecimalTextAndDates() {
        val decoded = decode(
            """{"monthly_rent_usd":1250.50,"available_date":"2026-09-01"}""",
            MarketplaceCategory.HOUSING
        )
        assertEquals(MarketplaceAttributeValue.Decimal("1250.50"), decoded["monthly_rent_usd"])
        assertEquals(MarketplaceAttributeValue.Date("2026-09-01"), decoded["available_date"])
    }

    @Test
    fun malformedOrMissingPayloadsShowNothingInsteadOfFailingTheRead() {
        assertEquals(MarketplaceAttributes.EMPTY, decode(null))
        assertEquals(MarketplaceAttributes.EMPTY, decode("[1]"))
        assertEquals(
            MarketplaceAttributes.EMPTY,
            decode("""{"ram_gb":"lots","device_type":"toaster","brand":{"a":1}}""")
        )
    }

    @Test
    fun valuesForOtherCategoriesAreIgnored() {
        assertEquals(MarketplaceAttributes.EMPTY, decode("""{"ram_gb":16}""", MarketplaceCategory.CLOTHING))
    }

    @Test
    fun invalidBoundsDatesAndInapplicableCapacitiesAreHidden() {
        assertEquals(
            setOf("device_type"),
            decode("""{"device_type":"monitor","ram_gb":16,"storage_gb":0}""").values.keys
        )
        assertEquals(
            MarketplaceAttributes.EMPTY,
            decode("""{"available_date":"2026-02-30","monthly_rent_usd":0}""", MarketplaceCategory.HOUSING)
        )
    }
}
