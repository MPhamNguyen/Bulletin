package com.jdrms.bulletin.domain.profile.infrastructure.mapper

import com.jdrms.bulletin.domain.profile.infrastructure.dto.ReviewDto
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReviewMapperTest {
    @Test
    fun deletedAuthorAndListingPreserveRatingOnlyReviewWithoutExposingStaleName() {
        val dto = Json.decodeFromString<ReviewDto>(
            """{"id":"review-1","reviewer_id":null,"reviewer_name":"Former author",
                "reviewee_id":"seller","score":4,"comment":"","listing_id":null,
                "was_listing_review":true,"created_at":"2026-10-25T12:00:00Z"}"""
        )

        val review = ReviewMapper.toDomain(dto)

        assertNull(review.reviewerId)
        assertNull(review.relatedListingId)
        assertEquals("Student", review.reviewerName)
        assertEquals(4, review.rating.score)
        assertEquals("", review.comment)
        assertTrue(review.wasListingReview)
        assertEquals(review, ReviewMapper.toDomain(ReviewMapper.toDto(review)))
    }

    @Test
    fun missingAndBlankNamesDefaultToStudent() {
        val row = Json.decodeFromString<ReviewDto>(
            """{"reviewer_id":"buyer","reviewee_id":"seller","score":5,"comment":"Great"}"""
        )
        assertNull(row.reviewerName)
        for (name in listOf(null, "", "  ")) {
            assertEquals("Student", ReviewMapper.toDomain(row.copy(reviewerName = name)).reviewerName)
        }
        assertEquals("Campus Buyer", ReviewMapper.toDomain(row.copy(reviewerName = " Campus Buyer ")).reviewerName)
    }

    @Test
    fun olderListingRowsWithoutOriginFieldStillAllowRatingOnlyReviews() {
        val dto = Json.decodeFromString<ReviewDto>(
            """{"reviewer_id":"buyer","reviewee_id":"seller","score":5,"comment":"","listing_id":"listing"}"""
        )

        assertTrue(ReviewMapper.toDomain(dto).wasListingReview)
    }

    @Test
    fun profileOnlyReviewsAllowEmptyComments() {
        val dto = ReviewDto(reviewerId = "buyer", revieweeId = "seller", score = 5, comment = " ")

        assertEquals(" ", ReviewMapper.toDomain(dto).comment)
        assertEquals("", ReviewMapper.toDomain(dto.copy(comment = "")).comment)
    }

    @Test
    fun scoresRemainClampedToRatingBounds() {
        val row = ReviewDto(reviewerId = "buyer", revieweeId = "seller", score = 10, comment = "Great")

        assertEquals(5, ReviewMapper.toDomain(row).rating.score)
        assertEquals(1, ReviewMapper.toDomain(row.copy(score = 0)).rating.score)
    }
}
