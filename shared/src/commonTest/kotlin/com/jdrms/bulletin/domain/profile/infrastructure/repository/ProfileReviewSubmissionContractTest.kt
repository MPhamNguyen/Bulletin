package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.domain.profile.application.SubmitStudentReview
import com.jdrms.bulletin.domain.profile.domain.model.Rating
import com.jdrms.bulletin.domain.profile.domain.model.ReviewId
import com.jdrms.bulletin.domain.profile.domain.model.StudentReview
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileReviewSubmissionContractTest {
    @Test
    fun ratingOnlyListingReviewIsSubmittedAndReadBackInBothRepositories() = runTest {
        verifySubmission("")
    }

    @Test
    fun listingReferenceIsAlsoPreservedForReviewsWithComments() = runTest {
        verifySubmission("Helpful seller")
    }

    @Test
    fun ratingOnlyProfileReviewIsSubmittedAndReadBackInBothRepositories() = runTest {
        verifySubmission("", listingId = null)
    }

    private suspend fun verifySubmission(comment: String, listingId: String? = "listing-1") {
        val fixture = ProfileReviewSubmissionFixture()
        try {
            fixture.signIn()
            val memory = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
            for (repository in listOf(memory, fixture.repository)) {
                val review = StudentReview(
                    id = ReviewId("new-review"),
                    reviewerId = FIRST_REVIEWER_ID,
                    reviewerName = "Buyer",
                    revieweeId = revieweeId,
                    rating = Rating(5),
                    comment = comment,
                    relatedListingId = listingId
                )
                SubmitStudentReview(repository)(revieweeId, review).getOrThrow()
                val stored = repository.getReputation(revieweeId).reviews.single()
                assertEquals(comment, stored.comment)
                assertEquals(listingId, stored.relatedListingId)
                assertEquals(listingId != null, stored.wasListingReview)
            }
        } finally {
            fixture.client.close()
        }
    }
}
