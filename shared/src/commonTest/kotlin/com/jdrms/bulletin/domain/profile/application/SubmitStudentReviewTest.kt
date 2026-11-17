package com.jdrms.bulletin.domain.profile.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.Rating
import com.jdrms.bulletin.domain.profile.domain.model.ReviewId
import com.jdrms.bulletin.domain.profile.domain.model.StudentReview
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SubmitStudentReviewTest {
    @Test
    fun retainedAnonymousReviewCannotBeSubmittedAsANewReview() = runTest {
        val repository = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val target = UserId("seller")
        val review = StudentReview(ReviewId("old-review"), null, "Student", target, Rating(5), "Great")

        assertIs<Result.Error>(SubmitStudentReview(repository)(target, review))
        assertTrue(repository.getReputation(target).reviews.isEmpty())
    }
}
