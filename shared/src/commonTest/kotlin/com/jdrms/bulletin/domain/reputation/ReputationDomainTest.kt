package com.jdrms.bulletin.domain.reputation

import com.jdrms.bulletin.domain.reputation.domain.model.*
import com.jdrms.bulletin.domain.reputation.domain.service.ReputationCalculationPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ReputationDomainTest {

    private val policy = ReputationCalculationPolicy()

    @Test
    fun testRatingBoundaries() {
        val validRating = Rating(5)
        assertEquals(5, validRating.score)

        assertFailsWith<IllegalArgumentException> {
            Rating(6)
        }
        assertFailsWith<IllegalArgumentException> {
            Rating(0)
        }
    }

    @Test
    fun testAverageCalculation() {
        val user = RevieweeId("user_101")
        val reviews = listOf(
            Review(ReviewId("r1"), ReviewerId("u1"), "User 1", user, Rating(5), "Great"),
            Review(ReviewId("r2"), ReviewerId("u2"), "User 2", user, Rating(4), "Good")
        )
        val rep = policy.calculateUserReputation(user, reviews)
        assertEquals(4.5, rep.averageRating)
        assertEquals(2, rep.totalReviews)
    }

    @Test
    fun testSelfReviewValidationFails() {
        val review = Review(
            id = ReviewId("r1"),
            reviewerId = ReviewerId("user_101"),
            reviewerName = "User",
            revieweeId = RevieweeId("user_101"),
            rating = Rating(5),
            comment = "Self review"
        )
        val result = policy.validateNewReview(review)
        assertTrue(result.isError())
    }
}
