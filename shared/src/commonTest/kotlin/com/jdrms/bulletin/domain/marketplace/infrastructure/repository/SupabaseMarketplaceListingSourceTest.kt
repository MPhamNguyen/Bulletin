package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.SupabaseMarketplaceListingDto
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class SupabaseMarketplaceListingSourceTest {

    @Test
    fun translatesTheExistingSupabaseListingsSchema() = runTest {
        val row = Json.decodeFromString<SupabaseMarketplaceListingDto>(
            """
            {
                "id":"c3a81234-5678-4abc-9def-123456789abc",
                "name":"Supabase Calculator",
                "user_id":"a3a81234-5678-4abc-9def-123456789abc",
                "category":"electronics",
                "condition":"good",
                "price":25.0,
                "description":"Seeded database listing",
                "created_at":"2026-09-20T12:34:56.123Z"
            }
            """.trimIndent()
        )
        val source = SupabaseMarketplaceListingSource {
            listOf(row)
        }

        val listings = source.getAvailableListings()

        assertEquals(1, listings.size)
        assertEquals("listing:c3a81234-5678-4abc-9def-123456789abc", listings.single().id)
        assertEquals("Supabase Calculator", listings.single().title)
        assertEquals("a3a81234-5678-4abc-9def-123456789abc", listings.single().sellerId)
        assertEquals("Student", listings.single().sellerName)
        assertEquals(MarketplaceCategory.ELECTRONICS, listings.single().category)
        assertEquals(1_789_907_696_123L, listings.single().createdAtMillis)
    }

    @Test
    fun translatesNullableOptionalColumnsAndPreventsNegativePrices() = runTest {
        val source = SupabaseMarketplaceListingSource {
            listOf(
                listingRow(
                    id = "unmapped",
                    category = null,
                    price = -5.0,
                    description = null
                )
            )
        }

        val listing = source.getAvailableListings().single()

        assertEquals(MarketplaceCategory.OTHER, listing.category)
        assertEquals(0.0, listing.priceAmount)
        assertEquals("", listing.description)
    }

    @Test
    fun excludesRowsMissingRequiredMarketplaceData() = runTest {
        val source = SupabaseMarketplaceListingSource {
            listOf(
                listingRow(id = "missing-seller", userId = null),
                listingRow(id = "missing-name", name = null),
                listingRow(id = "missing-price", price = null),
                listingRow(id = "valid")
            )
        }

        val listings = source.getAvailableListings()

        assertEquals(listOf("listing:valid"), listings.map { it.id })
    }

    private fun listingRow(
        id: String,
        userId: String? = "seller_1",
        name: String? = "Supabase Calculator",
        category: String? = "TEXTBOOKS",
        price: Double? = 25.0,
        description: String? = "Seeded database listing"
    ): SupabaseMarketplaceListingDto {
        return SupabaseMarketplaceListingDto(
            id = id,
            userId = userId,
            name = name,
            price = price,
            category = category,
            description = description
        )
    }
}
