package com.jdrms.bulletin.domain.listings

import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingCondition
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.ListingPrice
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.infrastructure.dto.SupabaseListingDto
import com.jdrms.bulletin.domain.listings.infrastructure.mapper.SupabaseListingMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ListingsInfrastructureTest {

    @Test
    fun mapsSupabaseListingToDomainTerms() {
        val listing = SupabaseListingMapper.toDomain(
            SupabaseListingDto(
                id = "listing-42",
                name = "Desk Lamp",
                userId = "seller-42",
                category = "electronics",
                condition = "like_new",
                price = 18.5,
                description = "Adjustable lamp",
                createdAt = "2026-09-21T12:00:00Z"
            )
        )

        assertEquals("listing-42", listing?.id?.value)
        assertEquals(SellerId("seller-42"), listing?.sellerId)
        assertEquals(ListingCategory.ELECTRONICS, listing?.category)
        assertEquals(ListingCondition.LIKE_NEW, listing?.condition)
        assertEquals(18.5, listing?.price?.amount)
        assertEquals("Adjustable lamp", listing?.description)
        assertTrue((listing?.createdAtMillis ?: 0L) > 0L)
    }

    @Test
    fun ignoresRowsWithoutRequiredListingFields() {
        assertNull(
            SupabaseListingMapper.toDomain(
                SupabaseListingDto(
                    id = "listing-42",
                    name = null,
                    userId = "seller-42",
                    price = 18.5
                )
            )
        )
        assertNull(
            SupabaseListingMapper.toDomain(
                SupabaseListingDto(
                    id = "listing-42",
                    name = "Desk Lamp",
                    userId = null,
                    price = 18.5
                )
            )
        )
    }

    @Test
    fun mapsDomainListingToSupabaseWriteShape() {
        val listing = Listing(
            id = ListingId("listing-42"),
            sellerId = SellerId("current_student"),
            sellerName = "Student",
            title = "Desk Lamp",
            description = "Adjustable lamp",
            price = ListingPrice(18.5),
            category = ListingCategory.ELECTRONICS,
            condition = ListingCondition.LIKE_NEW
        )

        val dto = SupabaseListingMapper.toInsertDto(listing, sellerId = "seller-42")

        assertEquals("listing-42", dto.id)
        assertEquals("seller-42", dto.userId)
        assertEquals("Desk Lamp", dto.name)
        assertEquals("ELECTRONICS", dto.category)
        assertEquals("LIKE_NEW", dto.condition)
        assertEquals(18.5, dto.price)
    }
}
