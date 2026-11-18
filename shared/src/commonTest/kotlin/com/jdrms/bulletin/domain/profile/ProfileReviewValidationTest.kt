package com.jdrms.bulletin.domain.profile

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.Rating
import com.jdrms.bulletin.domain.profile.domain.model.ReviewId
import com.jdrms.bulletin.domain.profile.domain.model.StudentReview
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.service.ProfileValidationPolicy
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class ProfileReviewValidationTest {
    private val policy = ProfileValidationPolicy()

    @Test
    fun newListingReviewCanContainOnlyARating() {
        for (comment in listOf("", "   ", "\t\n")) {
            assertIs<Result.Success<Unit>>(policy.validateNewReview(review(comment = comment)))
        }
    }

    @Test
    fun historicalOriginFlagCannotReplaceAListingReferenceForNewRatingOnlyReviews() {
        for (listingId in listOf(null, "", "   ")) {
            assertIs<Result.Error>(policy.validateNewReview(review(listingId = listingId)))
        }
    }

    @Test
    fun ratingOnlyReviewsStillRequireAReviewerAndCannotBeSelfReviews() {
        for (reviewerId in listOf(null, "", "   ", "seller")) {
            assertIs<Result.Error>(policy.validateNewReview(review(reviewerId = reviewerId)))
        }
    }

    @Test
    fun profileOnlyReviewStillRequiresAComment() {
        assertFailsWith<IllegalArgumentException> {
            review(listingId = null, wasListingReview = false)
        }
        assertIs<Result.Success<Unit>>(
            policy.validateNewReview(review(listingId = null, wasListingReview = false, comment = "Helpful seller"))
        )
    }

    private fun review(
        reviewerId: String? = "buyer",
        listingId: String? = "listing-1",
        wasListingReview: Boolean = true,
        comment: String = ""
    ) = StudentReview(
        id = ReviewId("review-1"),
        reviewerId = reviewerId,
        reviewerName = "Buyer",
        revieweeId = UserId("seller"),
        rating = Rating(5),
        comment = comment,
        relatedListingId = listingId,
        wasListingReview = wasListingReview
    )
}
