package com.jdrms.bulletin.domain.marketplace

import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.MarketplaceReviewDto
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MarketplaceReviewDtoTest {
    @Test
    fun retainedRatingOnlyReviewDecodesAfterAuthorAndListingDeletion() {
        val dto = Json.decodeFromString<MarketplaceReviewDto>(
            """{"id":"review","listing_id":null,"reviewer_id":null,"reviewee_id":"seller",
                "score":4,"comment":"","was_listing_review":true}"""
        )

        assertNull(dto.reviewerId)
        assertNull(dto.listingId)
        assertEquals(4, dto.score)
        assertTrue(dto.wasListingReview)
    }
}
