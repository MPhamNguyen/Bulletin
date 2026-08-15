package com.jdrms.bulletin.domain.reputation

import com.jdrms.bulletin.domain.reputation.application.GetStudentReputation
import com.jdrms.bulletin.domain.reputation.application.SubmitReview
import com.jdrms.bulletin.domain.reputation.domain.model.*
import com.jdrms.bulletin.domain.reputation.domain.service.ReputationCalculationPolicy
import com.jdrms.bulletin.domain.reputation.infrastructure.dto.ReviewDto
import com.jdrms.bulletin.domain.reputation.infrastructure.mapper.ReputationMapper
import com.jdrms.bulletin.domain.reputation.infrastructure.repository.InMemoryReputationRepository
import com.jdrms.bulletin.domain.reputation.presentation.ReputationViewModel
import kotlinx.coroutines.test.runTest
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

    @Test
    fun testInMemoryRepositorySubmitAndGet() = runTest {
        val repository = InMemoryReputationRepository(initialReviews = emptyMap())
        val getReputation = GetStudentReputation(repository, policy)
        val submit = SubmitReview(repository, policy)

        val target = RevieweeId("target_user")
        val reviewer = ReviewerId("reviewer_user")

        val newReview = Review(
            id = ReviewId("r_test"),
            reviewerId = reviewer,
            reviewerName = "Tester",
            revieweeId = target,
            rating = Rating(5),
            comment = "Awesome seller!"
        )

        val submitResult = submit(newReview)
        assertTrue(submitResult.isSuccess())

        val rep = getReputation(target)
        assertEquals(5.0, rep.averageRating)
        assertEquals(1, rep.totalReviews)
        assertEquals("Awesome seller!", rep.reviews.first().comment)
    }

    @Test
    fun testSubmitBlankCommentReturnsError() = runTest {
        val repository = InMemoryReputationRepository(initialReviews = emptyMap())
        val submit = SubmitReview(repository, policy)

        val badReview = Review(
            id = ReviewId("r_bad"),
            reviewerId = ReviewerId("u1"),
            reviewerName = "Tester",
            revieweeId = RevieweeId("u2"),
            rating = Rating(4),
            comment = "   "
        )

        val result = submit(badReview)
        assertTrue(result.isError())
    }

    @Test
    fun testMapperScoreClamping() {
        val outOfBoundsDto = ReviewDto(
            id = "dto_1",
            reviewerId = "u1",
            reviewerName = "User",
            revieweeId = "u2",
            score = 10, // out of range score from external DTO
            comment = "Clamped",
            createdAtMillis = 1000L
        )

        val domain = ReputationMapper.toDomain(outOfBoundsDto)
        assertEquals(5, domain.rating.score)
    }

    @Test
    fun testViewModelLoadReputationWithExplicitUserId() = runTest {
        val repository = InMemoryReputationRepository(
            initialReviews = mapOf(
                "other_user" to listOf(
                    ReviewDto(
                        id = "r99",
                        reviewerId = "reviewer",
                        reviewerName = "Reviewer",
                        revieweeId = "other_user",
                        score = 5,
                        comment = "Great profile",
                        createdAtMillis = 1000L
                    )
                )
            )
        )
        val getReputation = GetStudentReputation(repository, policy)
        val submit = SubmitReview(repository, policy)
        val viewModel = ReputationViewModel(getReputation, submit, targetUserId = RevieweeId("default_user"))

        viewModel.loadReputation(RevieweeId("other_user"))
        testScheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("other_user", state.userReputation?.userId?.value)
        assertEquals(5.0, state.userReputation?.averageRating)
    }
}
