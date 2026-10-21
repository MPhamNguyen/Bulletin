package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageRequest
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCondition
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.SupabaseMarketplaceListingDto
import io.github.jan.supabase.postgrest.PropertyConversionMethod
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

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
        val source = SupabaseMarketplaceListingSource { _ ->
            listOf(row)
        }

        val listings = source.getAvailableListings(MarketplacePageRequest()).listings

        assertEquals(1, listings.size)
        assertEquals("listing:c3a81234-5678-4abc-9def-123456789abc", listings.single().id)
        assertEquals("Supabase Calculator", listings.single().title)
        assertEquals("a3a81234-5678-4abc-9def-123456789abc", listings.single().sellerId)
        assertEquals("Student", listings.single().sellerName)
        assertEquals(MarketplaceCategory.ELECTRONICS, listings.single().category)
        assertEquals(1_789_907_696_123L, listings.single().createdAtMillis)
    }

    @Test
    fun mapsListingImagesInDisplayOrderWithCropSettings() = runTest {
        val row = Json.decodeFromString<SupabaseMarketplaceListingDto>(
            """
            {
                "id":"with-images",
                "name":"Lamp",
                "user_id":"seller",
                "price":10.0,
                "listing_images":[
                    {"object_path":"listing/second.webp","position":1},
                    {
                        "object_path":"listing/first.jpg",
                        "position":0,
                        "crop_scale":1.5,
                        "crop_offset_x":0.1,
                        "crop_offset_y":-0.2
                    }
                ]
            }
            """.trimIndent()
        )
        val source = SupabaseMarketplaceListingSource(
            fetch = { listOf(row) },
            imageUrl = { "https://cdn.example/$it" }
        )

        val photos = source.getAvailableListings(MarketplacePageRequest()).listings.single().photos

        assertEquals(
            listOf(
                "https://cdn.example/listing/first.jpg#bulletin_crop=1.5,0.1,-0.2",
                "https://cdn.example/listing/second.webp#bulletin_crop=1.0,0.0,0.0"
            ),
            photos
        )
    }

    @Test
    fun translatesNullableOptionalColumnsAndPreventsNegativePrices() = runTest {
        val source = SupabaseMarketplaceListingSource { _ ->
            listOf(
                listingRow(
                    id = "unmapped",
                    category = null,
                    price = -5.0,
                    description = null
                )
            )
        }

        val listing = source.getAvailableListings(MarketplacePageRequest()).listings.single()

        assertEquals(MarketplaceCategory.OTHER, listing.category)
        assertEquals(0.0, listing.priceAmount)
        assertEquals("", listing.description)
    }

    @Test
    fun excludesRowsMissingRequiredMarketplaceData() = runTest {
        val source = SupabaseMarketplaceListingSource { _ ->
            listOf(
                listingRow(id = "missing-seller", userId = null),
                listingRow(id = "missing-name", name = null),
                listingRow(id = "missing-price", price = null),
                listingRow(id = "valid")
            )
        }

        val listings = source.getAvailableListings(MarketplacePageRequest()).listings

        assertEquals(listOf("listing:valid"), listings.map { it.id })
    }

    @Test
    fun forwardsThePageRequestAndReturnsOnlyTheRequestedPageSize() = runTest {
        var capturedRequest: MarketplacePageRequest? = null
        val source = SupabaseMarketplaceListingSource { request ->
            capturedRequest = request
            listOf(listingRow(id = "second"), listingRow(id = "first"))
        }
        val request = MarketplacePageRequest(pageSize = 1)

        val page = source.getAvailableListings(request)

        assertEquals(request, capturedRequest)
        assertEquals(1, page.listings.size)
        assertNotNull(page.nextCursor)
    }

    @Test
    fun invalidLookaheadRowDoesNotIncorrectlyEndPagination() = runTest {
        val source = SupabaseMarketplaceListingSource { _ ->
            (1..20).map { index -> listingRow(id = "valid-$index") } +
                listingRow(id = "invalid-lookahead", userId = null)
        }

        val page = source.getAvailableListings(MarketplacePageRequest(pageSize = 20))

        assertEquals(20, page.listings.size)
        assertEquals("listing:valid-20", page.nextCursor?.itemId)
    }

    @Test
    fun fullPageKeepsPaginationActiveWhenServerCapsResultsAtPageSize() = runTest {
        val source = SupabaseMarketplaceListingSource { _ ->
            (1..20).map { index -> listingRow(id = "listing-$index") }
        }

        val page = source.getAvailableListings(MarketplacePageRequest(pageSize = 20))

        assertEquals(20, page.listings.size)
        assertEquals("listing:listing-20", page.nextCursor?.itemId)
    }

    @Test
    fun cursorPreservesSupabaseTimestampPrecision() = runTest {
        val preciseTimestamp = "2026-09-21T00:55:08.593977+00:00"
        val source = SupabaseMarketplaceListingSource { _ ->
            listOf(listingRow(id = "precise", createdAt = preciseTimestamp))
        }

        val page = source.getAvailableListings(MarketplacePageRequest(pageSize = 1))

        assertEquals(kotlin.time.Instant.parse(preciseTimestamp), page.nextCursor?.createdAt)
    }

    @Test
    fun trustsDatabaseCursorOrderingWithoutFilteringMappedMillisecondsAgain() = runTest {
        val cursorTimestamp = "2026-09-21T00:55:08.593977Z"
        val source = SupabaseMarketplaceListingSource { _ ->
            listOf(listingRow(id = "z-row", createdAt = "2026-09-21T00:55:08.593500Z"))
        }
        val request = MarketplacePageRequest(
            cursor = com.jdrms.bulletin.domain.marketplace.application.MarketplacePageCursor(
                createdAt = kotlin.time.Instant.parse(cursorTimestamp),
                itemId = "listing:a-row"
            )
        )

        val page = source.getAvailableListings(request)

        assertEquals(listOf("listing:z-row"), page.listings.map { it.id })
    }

    @Test
    fun searchBuildsSanitizedPrefixTermsForUntrustedKeywords() {
        val builder = PostgrestFilterBuilder(PropertyConversionMethod.NONE)
        val query = "desk,lamp)\\\"%_\\\\"

        builder.applyMarketplaceSearch(
            MarketplacePageRequest(
                query = query,
                sellerId = "seller-42",
                category = MarketplaceCategory.TEXTBOOKS,
                condition = MarketplaceCondition.LIKE_NEW
            )
        )

        assertEquals(listOf("eq.seller-42"), builder.params["user_id"])
        assertEquals(listOf("ilike.TEXTBOOKS"), builder.params["category"])
        assertEquals(listOf("ilike.LIKE_NEW"), builder.params["condition"])
        assertEquals(listOf("fts.desk:* & lamp:*"), builder.params["name"])
        assertFalse("or" in builder.params)
    }

    @Test
    fun marketplaceQueryExcludesTheViewerListings() {
        val builder = PostgrestFilterBuilder(PropertyConversionMethod.NONE)

        builder.applyMarketplaceSearch(MarketplacePageRequest(viewerId = "viewer-42"))

        assertEquals(listOf("neq.viewer-42"), builder.params["user_id"])
    }

    @Test
    fun marketplaceQueryReturnsNoOwnedListingsWhenSellerMatchesViewer() {
        val builder = PostgrestFilterBuilder(PropertyConversionMethod.NONE)

        builder.applyMarketplaceSearch(
            MarketplacePageRequest(viewerId = "viewer-42", sellerId = "viewer-42")
        )

        assertEquals(listOf("neq.viewer-42", "eq.viewer-42"), builder.params["user_id"])
    }

    @Test
    fun partialSearchTermUsesPrefixMatching() {
        val builder = PostgrestFilterBuilder(PropertyConversionMethod.NONE)

        builder.applyMarketplaceSearch(
            MarketplacePageRequest(query = "c", category = MarketplaceCategory.TEXTBOOKS)
        )

        assertEquals(listOf("fts.c:*"), builder.params["name"])
    }

    private fun listingRow(
        id: String,
        userId: String? = "seller_1",
        name: String? = "Supabase Calculator",
        category: String? = "TEXTBOOKS",
        price: Double? = 25.0,
        description: String? = "Seeded database listing",
        createdAt: String? = null
    ): SupabaseMarketplaceListingDto {
        return SupabaseMarketplaceListingDto(
            id = id,
            userId = userId,
            name = name,
            price = price,
            category = category,
            description = description,
            createdAt = createdAt
        )
    }
}
