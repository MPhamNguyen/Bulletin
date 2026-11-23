package com.jdrms.bulletin.domain.profile.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.ReviewId
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PublishStudentReviewTest {
    @Test
    fun publishReviewValidatesInputsAndUsesProvidedIdentityAndClock() = runTest {
        val repository = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val publish = PublishStudentReview(
            SubmitStudentReview(repository),
            newId = { ReviewId("review-1") },
            nowMillis = { 42L }
        )
        val reviewer = UserId("reviewer")
        val target = UserId("target")
        assertIs<Result.Error>(publish(target, reviewer, "Reviewer", 0, "Good"))
        assertIs<Result.Error>(publish(reviewer, reviewer, "Reviewer", 5, "Good"))
        assertIs<Result.Success<Unit>>(publish(target, reviewer, "Reviewer", 5, " Good "))
        val review = repository.getReputation(target).reviews.single()
        assertEquals("reviewer", review.reviewerId)
        assertEquals("Good", review.comment)
        assertEquals(42L, review.createdAtMillis)
    }

    @Test
    fun publishProfileReviewAcceptsEmptyAndWhitespaceComments() = runTest {
        for (comment in listOf("", "   ", "\t\n")) {
            val repository = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
            val target = UserId("target")
            val publish = PublishStudentReview(SubmitStudentReview(repository))

            assertIs<Result.Success<Unit>>(publish(target, UserId("reviewer"), "Reviewer", 4, comment))

            val review = repository.getReputation(target).reviews.single()
            assertEquals("", review.comment)
            assertEquals(4, review.rating.score)
        }
    }
}
