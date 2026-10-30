package com.jdrms.bulletin.domain.listings.attributes

import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributes
import com.jdrms.bulletin.domain.listings.infrastructure.dto.SupabaseListingDto
import com.jdrms.bulletin.domain.listings.infrastructure.mapper.SupabaseListingMapper
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SupabaseListingAttributesMappingTest {
    private fun dto(attributes: String?, category: String = "ELECTRONICS") = SupabaseListingDto(
        id = "11111111-1111-1111-1111-111111111111",
        name = "Laptop",
        userId = "seller-1",
        category = category,
        condition = "GOOD",
        price = 10.0,
        description = "d",
        attributes = attributes?.let(Json::parseToJsonElement)
    )

    @Test
    fun writeDtoCarriesTypedAttributesAndAlwaysEncodesEmptyObjects() {
        val listing = attributeListing()
        assertEquals(
            laptopAttributes,
            SupabaseListingMapper.toDomain(
                dto(SupabaseListingMapper.toInsertDto(listing, "seller-1").attributes.toString())
            )?.attributes
        )
        val cleared = SupabaseListingMapper.toInsertDto(listing.copy(attributes = ListingAttributes.EMPTY), "seller-1")
        assertEquals("{}", cleared.attributes.toString())
    }

    @Test
    fun legacyRowsWithoutTheColumnReadAsEmptyAttributes() {
        assertEquals(ListingAttributes.EMPTY, SupabaseListingMapper.toDomain(dto(null))?.attributes)
    }

    @Test
    fun unknownValuesSurviveReadAndWrite() {
        val listing = SupabaseListingMapper.toDomain(dto("""{"brand":"Dell","future":"kept"}"""))
        assertNotNull(listing)
        val written = SupabaseListingMapper.toInsertDto(listing.copy(title = "Renamed"), "seller-1")
        assertEquals(Json.parseToJsonElement("""{"brand":"Dell","future":"kept"}"""), written.attributes)
    }

    @Test
    fun malformedPayloadMakesTheRowUnreadableInsteadOfBecomingEmpty() {
        assertNull(SupabaseListingMapper.toDomain(dto("[1,2]")))
        assertNull(SupabaseListingMapper.toDomain(dto("\"oops\"")))
    }
}
