package com.jdrms.bulletin.domain.listings.attributes

import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeCatalog
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeKeys
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeType
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeValue
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributes
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeInputParser
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeValidationPolicy
import com.jdrms.bulletin.domain.listings.domain.service.ListingMultiChoiceInput
import com.jdrms.bulletin.domain.listings.infrastructure.mapper.ListingAttributesDecoding
import com.jdrms.bulletin.domain.listings.infrastructure.mapper.ListingAttributesJsonCodec
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ListingColorMultiSelectTest {
    private val parser = ListingAttributeInputParser()
    private val codec = ListingAttributesJsonCodec()
    private val key = ListingAttributeKeys.COLOR
    private val options = ListingAttributeCatalog.definition(ListingCategory.CLOTHING, key)
        ?.let { (it.type as ListingAttributeType.MultiChoice).options }
        .orEmpty()

    private fun decode(json: String): ListingAttributes {
        val decoded = codec.decode(Json.parseToJsonElement(json), ListingCategory.CLOTHING)
        return assertIs<ListingAttributesDecoding.Decoded>(decoded).attributes
    }

    @Test
    fun aListingCanCarryManyColorsStoredInCatalogOrder() {
        val result = parser.parse(ListingCategory.CLOTHING, mapOf(key to "blue,red,beige"))

        assertEquals(
            listOf("red", "blue", "beige"),
            (result.attributes?.get(key) as ListingAttributeValue.MultiChoice).codes.toList()
        )
    }

    @Test
    fun anyUnknownColorRejectsTheWholeSelection() {
        val result = parser.parse(ListingCategory.CLOTHING, mapOf(key to "red,teal"))

        assertNull(result.attributes)
        assertEquals("Choose valid color options.", result.errors[key])
    }

    @Test
    fun blankColorIsOmittedBecauseTheFieldIsOptional() {
        val result = parser.parse(ListingCategory.CLOTHING, mapOf(key to ""))

        assertNull(result.attributes?.get(key))
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun validationRejectsAnEmptyOrForeignColorSet() {
        val policy = ListingAttributeValidationPolicy()
        fun issues(codes: Set<String>) = policy.validate(
            ListingCategory.CLOTHING,
            ListingAttributes(mapOf(key to ListingAttributeValue.MultiChoice(codes)))
        )

        assertTrue(issues(setOf("red", "blue")).isEmpty())
        assertEquals(listOf(key), issues(emptySet()).map { it.key })
        assertEquals(listOf(key), issues(setOf("red", "teal")).map { it.key })
    }

    @Test
    fun colorsAreStoredAsAJsonArrayAndRoundTrip() {
        val attributes = ListingAttributes(mapOf(key to ListingAttributeValue.MultiChoice(setOf("red", "blue"))))

        val encoded = codec.encode(attributes).toString()

        assertEquals("""{"color":["red","blue"]}""", encoded)
        assertEquals(attributes, decode(encoded))
    }

    @Test
    fun aLegacySingleColorStringIsReadAsOneColor() {
        assertEquals(ListingAttributeValue.MultiChoice(setOf("green")), decode("""{"color":"green"}""")[key])
    }

    @Test
    fun malformedColorArraysAreKeptVerbatimNotShown() {
        val attributes = decode("""{"color":["red",3]}""")

        assertNull(attributes[key])
        assertEquals("""["red",3]""", attributes.preserved[key])
        assertNull(decode("""{"color":[]}""")[key])
    }

    @Test
    fun togglingKeepsCatalogOrderAndDeselectsOnSecondTap() {
        val afterBlue = ListingMultiChoiceInput.toggle("", "blue", options)
        val afterRed = ListingMultiChoiceInput.toggle(afterBlue, "red", options)

        assertEquals("red,blue", afterRed)
        assertEquals("blue", ListingMultiChoiceInput.toggle(afterRed, "red", options))
    }
}
