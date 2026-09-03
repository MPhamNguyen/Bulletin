package com.jdrms.bulletin.domain.profile

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.Rating
import com.jdrms.bulletin.domain.profile.domain.model.ReviewId
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.StudentReview
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.service.ProfileValidationPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
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
    fun testValidNonUniversityEmailsMatchRegex() {
        val gmail = StudentEmail("jane.doe@gmail.com")
        assertFalse(gmail.isUniversityEmail)
        assertEquals("jane.doe@gmail.com", gmail.value)

        assertTrue(StudentEmail.isValid("user_name+tag@sub.domain.org"))
        assertTrue(StudentEmail.isValid("simple@example.com"))
    }

    @Test
    fun testInvalidEmailThrowsException() {
        assertFailsWith<IllegalArgumentException> {
            StudentEmail("not-an-email")
        }
        assertFailsWith<IllegalArgumentException> {
            StudentEmail("user@")
        }
        assertFailsWith<IllegalArgumentException> {
            StudentEmail("@domain.com")
        }
        assertFailsWith<IllegalArgumentException> {
            StudentEmail("user@domain")
        }
    }

    @Test
    fun testValidateRegistrationSuccess() {
        val result = policy.validateRegistration(
            emailStr = "student@gmail.com",
            password = "securePassword123",
            fullName = "Jane Student"
        )
        assertTrue(result.isSuccess())
    }

    @Test
    fun testValidateRegistrationFailsForBlankName() {
        val result = policy.validateRegistration(
            emailStr = "student@gmail.com",
            password = "securePassword123",
            fullName = "   "
        )
        assertTrue(result.isError())
        assertEquals("Full name is required.", (result as Result.Error).exception.message)
    }

    @Test
    fun testValidateRegistrationFailsForEmptyEmail() {
        val result = policy.validateRegistration(
            emailStr = "",
            password = "securePassword123",
            fullName = "Jane Student"
        )
        assertTrue(result.isError())
        assertEquals("Email is required.", (result as Result.Error).exception.message)
    }

    @Test
    fun testValidateRegistrationFailsForInvalidEmail() {
        val result = policy.validateRegistration(
            emailStr = "invalid-email-address",
            password = "securePassword123",
            fullName = "Jane Student"
        )
        assertTrue(result.isError())
        assertEquals("Invalid email address format.", (result as Result.Error).exception.message)
    }

    @Test
    fun testValidateRegistrationFailsForEmptyPassword() {
        val result = policy.validateRegistration(
            emailStr = "student@gmail.com",
            password = "",
            fullName = "Jane Student"
        )
        assertTrue(result.isError())
        assertEquals("Password is required.", (result as Result.Error).exception.message)
    }

    @Test
    fun testValidateRegistrationFailsForShortPassword() {
        val result = policy.validateRegistration(
            emailStr = "student@gmail.com",
            password = "1234567",
            fullName = "Jane Student"
        )
        assertTrue(result.isError())
        assertEquals("Password must be at least 8 characters.", (result as Result.Error).exception.message)
    }

    @Test
    fun testStudentEmailTrimsWhitespaceAndPreservesEquality() {
        val email1 = StudentEmail("  dominic@csulb.edu  ")
        val email2 = StudentEmail("dominic@csulb.edu")
        val emailUpper = StudentEmail("DOMINIC@CSULB.EDU")
        assertEquals("dominic@csulb.edu", email1.value)
        assertEquals(email1, email2)
        assertEquals(email1, emailUpper)
        assertEquals(email1.hashCode(), email2.hashCode())
        assertEquals(email1.hashCode(), emailUpper.hashCode())
    }

    @Test
    fun testNonEduEmailFailsUniversityValidation() {
        val result = policy.validateUniversityRegistration("user@gmail.com")
        assertTrue(result.isError())
    }

    @Test
    fun testEduEmailPassesUniversityValidation() {
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
    fun testStudentProfileUpdatesEditableDetails() {
        val profile = StudentProfile(
            id = UserId("student_1"),
            email = StudentEmail("student@example.com"),
            fullName = "Original Name"
        )

        val result = profile.updateDetails(
            fullName = "  John Doe  ",
            major = "  Computer Science  ",
            university = "  California State University - Long Beach  ",
            bio = "  Campus seller and student.  "
        )

        assertTrue(result is Result.Success)
        assertEquals("John Doe", result.data.fullName)
        assertEquals("Computer Science", result.data.major)
        assertEquals("California State University - Long Beach", result.data.university)
        assertEquals("Campus seller and student.", result.data.bio)
        assertEquals("Original Name", profile.fullName)
    }

    @Test
    fun testStudentProfileRejectsInvalidEditableDetails() {
        val profile = StudentProfile(
            id = UserId("student_1"),
            email = StudentEmail("student@example.com"),
            fullName = "Original Name"
        )

        val blankName = profile.updateDetails(" ", "Computer Science", "CSULB", "Bio")
        assertTrue(blankName is Result.Error)
        assertEquals("Full name is required.", blankName.exception.message)

        val blankSchool = profile.updateDetails("John Doe", "Computer Science", " ", "Bio")
        assertTrue(blankSchool is Result.Error)
        assertEquals("School is required.", blankSchool.exception.message)

        val tooLongBio = profile.updateDetails("John Doe", "Computer Science", "CSULB", "a".repeat(501))
        assertTrue(tooLongBio is Result.Error)
        assertEquals("Bio must be 500 characters or fewer.", tooLongBio.exception.message)
    }

    @Test
    fun testPolicyValidationMethods() {
        val loginResult = policy.validateLogin("invalid-email", "pass")
        assertTrue(loginResult.isError())
        assertEquals("Invalid email address format.", (loginResult as Result.Error).exception.message)

        val regFirstNameResult = policy.validateRegistration("", "Last", "test@csulb.edu", "password123")
        assertTrue(regFirstNameResult.isError())
        assertEquals("First name is required.", (regFirstNameResult as Result.Error).exception.message)

        val regLastNameResult = policy.validateRegistration("First", "", "test@csulb.edu", "password123")
        assertTrue(regLastNameResult.isError())
        assertEquals("Last name is required.", (regLastNameResult as Result.Error).exception.message)
    }
}
