package com.jdrms.bulletin.domain.listings.attributes

import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeValue
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributes
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.infrastructure.mapper.ListingAttributesDecoding
import com.jdrms.bulletin.domain.listings.infrastructure.mapper.ListingAttributesJsonCodec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ListingAttributesJsonCodecTest {
    private val codec = ListingAttributesJsonCodec()

    private fun decode(json: String, category: ListingCategory = ListingCategory.ELECTRONICS): ListingAttributes {
        val decoded = codec.decode(Json.parseToJsonElement(json), category)
        return assertIs<ListingAttributesDecoding.Decoded>(decoded).attributes
    }

    @Test
    fun encodesTypedNumbersNotStrings() {
        assertEquals(
            """{"brand":"Framework","device_type":"laptop","ram_gb":16,"storage_gb":512}""",
            codec.encode(laptopAttributes).toString()
        )
    }

    @Test
    fun decimalMoneyKeepsExactTextAndDatesAndIsbnStayText() {
        val encoded = codec.encode(housingAttributes).toString()
        assertEquals("""{"monthly_rent_usd":1250.50,"available_date":"2026-09-01","housing_type":"room"}""", encoded)
        assertEquals(housingAttributes, decode(encoded, ListingCategory.HOUSING))
        val book = decode("""{"isbn":"0306406152","edition":3}""", ListingCategory.TEXTBOOKS)
        assertEquals(ListingAttributeValue.Text("0306406152"), book["isbn"])
        assertEquals(ListingAttributeValue.Integer(3), book["edition"])
    }

    @Test
    fun roundTripReturnsTheSamePayload() {
        val json = """{"brand":"Framework","device_type":"laptop","ram_gb":16,"storage_gb":512}"""
        assertEquals(json, codec.encode(decode(json)).toString())
    }

    @Test
    fun legacyAbsentOrEmptyPayloadsDecodeAsEmpty() {
        assertEquals(
            ListingAttributes.EMPTY,
            assertIs<ListingAttributesDecoding.Decoded>(codec.decode(null, ListingCategory.OTHER)).attributes
        )
        assertEquals(ListingAttributes.EMPTY, decode("{}"))
    }

    @Test
    fun unknownAndNestedValuesArePreservedVerbatimAndWrittenBack() {
        val json = """{"brand":"Dell","future_key":"kept","future_obj":{"a":[1,2]}}"""
        val attributes = decode(json)
        assertEquals(setOf("future_key", "future_obj"), attributes.preserved.keys)
        assertEquals(Json.parseToJsonElement(json), codec.encode(attributes))
    }

    @Test
    fun invalidStoredValuesAreQuarantinedNotErasedOrRewritten() {
        val json = """{"device_type":"laptop","ram_gb":"lots","storage_gb":0,"brand":""}"""
        val attributes = decode(json)
        assertEquals(setOf("device_type"), attributes.values.keys)
        assertEquals(setOf("ram_gb", "storage_gb", "brand"), attributes.preserved.keys)
        assertEquals(JsonPrimitive(0).toString(), attributes.preserved.getValue("storage_gb"))
        assertEquals(Json.parseToJsonElement(json), codec.encode(attributes))
    }

    @Test
    fun keysThatDoNotApplyToTheCategoryAreHeldBackAsPreserved() {
        val attributes = decode("""{"device_type":"laptop","ram_gb":16}""", ListingCategory.CLOTHING)
        assertTrue(attributes.values.isEmpty())
        assertEquals(setOf("device_type", "ram_gb"), attributes.preserved.keys)
    }

    @Test
    fun capacityWithoutSuitableDeviceIsQuarantined() {
        val attributes = decode("""{"device_type":"monitor","ram_gb":16}""")
        assertEquals(setOf("device_type"), attributes.values.keys)
        assertEquals(setOf("ram_gb"), attributes.preserved.keys)
    }

    @Test
    fun nonObjectPayloadIsReportedMalformedNotEmpty() {
        listOf("[]", "\"text\"", "12").forEach { json ->
            assertIs<ListingAttributesDecoding.Malformed>(
                codec.decode(Json.parseToJsonElement(json), ListingCategory.OTHER)
            )
        }
        assertIs<JsonObject>(codec.encode(ListingAttributes.EMPTY))
    }
}
