package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceSellerReview
import kotlin.test.Test
import kotlin.test.assertEquals

class SellerProfileReviewsTest {
    @Test
    fun profileShowsOnlyThreeMostRecentReviews() {
        val reviews = (1..5).map { index ->
            MarketplaceSellerReview(
                id = "review-$index",
                reviewerName = "Student $index",
                rating = 5,
                comment = "Review $index",
                createdAtMillis = index.toLong()
            )
        }

        assertEquals(
            listOf("review-5", "review-4", "review-3"),
            mostRecentSellerReviews(reviews).map { it.id }
        )
    }
}
