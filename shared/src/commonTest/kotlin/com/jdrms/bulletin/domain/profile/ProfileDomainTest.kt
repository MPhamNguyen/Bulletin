package com.jdrms.bulletin.domain.profile

import com.jdrms.bulletin.domain.profile.domain.model.Rating
import com.jdrms.bulletin.domain.profile.domain.model.ReviewId
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentReview
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.service.ProfileValidationPolicy
import com.jdrms.bulletin.domain.profile.infrastructure.dto.ReviewDto
import com.jdrms.bulletin.domain.profile.infrastructure.mapper.ProfileMapper
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ProfileDomainTest {

    private val policy = ProfileValidationPolicy()

    @Test
    fun testValidUniversityEmail() {
        val email = StudentEmail("dominic@csulb.edu")
        assertTrue(email.isUniversityEmail)
        assertEquals("dominic@csulb.edu", email.value)
    }

    @Test
    fun testInvalidEmailThrowsException() {
        assertFailsWith<IllegalArgumentException> {
            StudentEmail("not-an-email")
        }
    }

    @Test
    fun testNonEduEmailFailsValidation() {
        val result = policy.validateUniversityRegistration("user@gmail.com")
        assertTrue(result.isError())
    }

    @Test
    fun testEduEmailPassesValidation() {
        val result = policy.validateUniversityRegistration("student@csulb.edu")
        assertTrue(result.isSuccess())
    }

    @Test
    fun testRatingBoundaries() {
        val rating = Rating(5)
        assertEquals(5, rating.score)

        assertFailsWith<IllegalArgumentException> { Rating(6) }
        assertFailsWith<IllegalArgumentException> { Rating(0) }
    }

    @Test
    fun testSelfReviewValidationFails() {
        val review = StudentReview(
            id = ReviewId("r1"),
            reviewerId = "user_101",
            reviewerName = "Dominic",
            revieweeId = UserId("user_101"),
            rating = Rating(5),
            comment = "Self review"
        )
        val result = policy.validateNewReview(review)
        assertTrue(result.isError())
    }

    @Test
    fun testCalculateReputationAverage() {
        val user = UserId("user_1")
        val reviews = listOf(
            StudentReview(ReviewId("r1"), "u2", "Sean", user, Rating(5), "Great!"),
            StudentReview(ReviewId("r2"), "u3", "Jacob", user, Rating(4), "Good!")
        )
        val rep = policy.calculateReputation(user, reviews)
        assertEquals(4.5, rep.averageRating)
        assertEquals(2, rep.totalReviews)
    }

    @Test
    fun testProfileRepositorySubmitAndGetReputation() = runTest {
        val repo = InMemoryProfileRepository(
            initialProfiles = emptyMap(),
            initialReviews = emptyMap()
        )
        val target = UserId("target_student")
        val review = StudentReview(
            id = ReviewId("rev_test"),
            reviewerId = "peer_1",
            reviewerName = "Peer Reviewer",
            revieweeId = target,
            rating = Rating(5),
            comment = "Smooth campus transaction!"
        )

        val submitResult = repo.submitReview(target, review)
        assertTrue(submitResult.isSuccess())

        val rep = repo.getReputation(target)
        assertEquals(5.0, rep.averageRating)
        assertEquals(1, rep.totalReviews)
        assertEquals("Smooth campus transaction!", rep.reviews.first().comment)
    }

    @Test
    fun testMapperScoreClamping() {
        val dto = ReviewDto(
            id = "dto_r1",
            reviewerId = "u1",
            reviewerName = "Sean",
            revieweeId = "u2",
            score = 10,
            comment = "Great"
        )
        val domain = ProfileMapper.toDomain(dto)
        assertEquals(5, domain.rating.score)
    }
}
