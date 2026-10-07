package com.jdrms.bulletin.domain.profile

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.ProfilePhoto
import com.jdrms.bulletin.domain.profile.domain.model.ProfilePhotoUrl
import com.jdrms.bulletin.domain.profile.domain.model.Rating
import com.jdrms.bulletin.domain.profile.domain.model.ReviewId
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.StudentReview
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.service.ProfileValidationPolicy
import com.jdrms.bulletin.domain.profile.infrastructure.dto.ReviewDto
import com.jdrms.bulletin.domain.profile.infrastructure.mapper.ProfileMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileDomainTest {

    private val policy = ProfileValidationPolicy()

    @Test
    fun profilePhotoAcceptsSupportedImagesAndDefensivelyCopiesBytes() {
        val source = pngBytes()
        val result = ProfilePhoto.create(source, "image/png")

        assertTrue(result is Result.Success)
        source[0] = 9
        val returned = result.data.bytes
        returned[1] = 9
        assertEquals(0x89.toByte(), result.data.bytes[0])
        assertEquals(0x50.toByte(), result.data.bytes[1])
    }

    @Test
    fun profilePhotoRejectsEmptyOversizedAndUnsupportedContent() {
        assertTrue(ProfilePhoto.create(byteArrayOf(), "image/jpeg") is Result.Error)
        assertTrue(ProfilePhoto.create(ByteArray(ProfilePhoto.MAX_SIZE_BYTES + 1), "image/webp") is Result.Error)
        assertTrue(ProfilePhoto.create(byteArrayOf(1), "image/gif") is Result.Error)
    }

    @Test
    fun profilePhotoRejectsSpoofedAndMismatchedImageContent() {
        assertTrue(ProfilePhoto.create(byteArrayOf(1, 2, 3), "image/jpeg") is Result.Error)
        assertTrue(ProfilePhoto.create(pngBytes(), "image/jpeg") is Result.Error)
    }

    @Test
    fun profilePhotoRecognizesEverySupportedImageSignature() {
        val jpeg = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 0x01)
        val webp = byteArrayOf(
            0x52,
            0x49,
            0x46,
            0x46,
            0x04,
            0x00,
            0x00,
            0x00,
            0x57,
            0x45,
            0x42,
            0x50
        )

        assertTrue(ProfilePhoto.create(jpeg, "image/jpeg") is Result.Success)
        assertTrue(ProfilePhoto.create(pngBytes(), "image/png") is Result.Success)
        assertTrue(ProfilePhoto.create(webp, "image/webp") is Result.Success)
    }

    @Test
    fun studentProfileChangesItsPhotoThroughTheAggregateOperation() {
        val profile = StudentProfile(
            id = UserId("student_1"),
            email = StudentEmail("student@example.com"),
            fullName = "Student"
        )

        val updated = profile.changeProfilePhoto(ProfilePhotoUrl("https://example.com/avatar.jpg"))

        assertEquals("https://example.com/avatar.jpg", updated.avatarUrl)
        assertEquals(null, profile.avatarUrl)
    }

    @Test
    fun testValidUniversityEmail() {
        val email = StudentEmail("dominic@csulb.edu")
        assertTrue(email.isUniversityEmail)
        assertEquals("dominic@csulb.edu", email.value)
    }

    @Test
    fun confirmingEmailPreservesProfileIdentityAndIsIdempotent() {
        val profile = StudentProfile(
            id = UserId("student-id"),
            email = StudentEmail("student@school.edu"),
            fullName = "Student Name"
        )

        val confirmed = profile.confirmEmail()

        assertTrue(confirmed.isVerified)
        assertEquals(profile.id, confirmed.id)
        assertEquals(profile.email, confirmed.email)
        assertEquals(confirmed, confirmed.confirmEmail())
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
            emailStr = "bob.smith@student.school.edu",
            password = "securePassword123",
            fullName = "Jane Student"
        )
        assertTrue(result.isSuccess())
    }

    @Test
    fun testValidateRegistrationRejectsNonEduEmail() {
        val result = policy.validateRegistration(
            emailStr = "student@gmail.com",
            password = "securePassword123",
            fullName = "Jane Student"
        )

        assertTrue(result.isError())
        assertEquals(
            "Bulletin requires a valid .edu university email.",
            (result as Result.Error).exception.message
        )
    }

    @Test
    fun testValidateRegistrationWithSeparateNamesRequiresEduEmail() {
        val result = policy.validateRegistration(
            firstName = "Jane",
            lastName = "Student",
            emailStr = "student@example.com",
            password = "securePassword123"
        )

        assertTrue(result.isError())
        assertEquals(
            "Bulletin requires a valid .edu university email.",
            (result as Result.Error).exception.message
        )
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
            bio = "  Campus seller and student.  ",
            graduationDate = "  Class of 2025  "
        )

        assertTrue(result is Result.Success)
        assertEquals("John Doe", result.data.fullName)
        assertEquals("Computer Science", result.data.major)
        assertEquals("Class of 2025", result.data.graduationDate)
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

        val longBio = profile.updateDetails(
            "John Doe",
            "Computer Science",
            "CSULB",
            "x".repeat(StudentProfile.MAX_BIO_LENGTH + 1)
        )
        assertTrue(longBio is Result.Error)
        assertEquals("Bio must be 500 characters or fewer.", longBio.exception.message)

        val longGradDate = profile.updateDetails(
            fullName = "John Doe",
            major = "Computer Science",
            university = "CSULB",
            bio = "Bio",
            graduationDate = "x".repeat(StudentProfile.MAX_GRAD_DATE_LENGTH + 1)
        )
        assertTrue(longGradDate is Result.Error)
        assertEquals("Graduation date must be 50 characters or fewer.", longGradDate.exception.message)
    }

    @Test
    fun testProfileMapperPreservesMajorAndGraduationDate() {
        val profile = StudentProfile(
            id = UserId("student_1"),
            email = StudentEmail("student@example.com"),
            fullName = "John Doe",
            major = "Computer Science",
            graduationDate = "Class of 2025"
        )

        val dto = ProfileMapper.toDto(profile)
        assertEquals("Computer Science", dto.major)
        assertEquals("Class of 2025", dto.graduationDate)
        assertEquals(profile, ProfileMapper.toDomain(dto))
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

    private fun pngBytes(): ByteArray = byteArrayOf(
        0x89.toByte(),
        0x50,
        0x4e,
        0x47,
        0x0d,
        0x0a,
        0x1a,
        0x0a,
        0x01
    )
    @Test
    fun testSoftDeleteSetsTimestampAndIsDeleted() {
        val profile = StudentProfile(
            id = UserId("student_1"),
            email = StudentEmail("student@example.com"),
            fullName = "John Doe"
        )
        assertFalse(profile.isDeleted)
        assertNull(profile.deletedAt)

        val timestamp = "2026-10-06T19:00:00Z"
        val result = profile.softDelete(timestamp)

        assertTrue(result is Result.Success)
        val deleted = result.data
        assertTrue(deleted.isDeleted)
        assertEquals(timestamp, deleted.deletedAt)
    }

    @Test
    fun testSoftDeleteRejectsAlreadyDeletedOrBlankTimestamp() {
        val profile = StudentProfile(
            id = UserId("student_1"),
            email = StudentEmail("student@example.com"),
            fullName = "John Doe",
            deletedAt = "2026-10-06T19:00:00Z"
        )
        assertTrue(profile.isDeleted)

        val reDeleteResult = profile.softDelete("2026-10-06T19:05:00Z")
        assertTrue(reDeleteResult is Result.Error)
        assertEquals("Profile is already deleted.", reDeleteResult.exception.message)

        val activeProfile = StudentProfile(
            id = UserId("student_2"),
            email = StudentEmail("student2@example.com"),
            fullName = "Jane Doe"
        )
        val blankResult = activeProfile.softDelete("   ")
        assertTrue(blankResult is Result.Error)
        assertEquals("Deletion timestamp cannot be blank.", blankResult.exception.message)
    }

    @Test
    fun testUpdateDetailsRejectsSoftDeletedProfile() {
        val profile = StudentProfile(
            id = UserId("student_1"),
            email = StudentEmail("student@example.com"),
            fullName = "John Doe",
            deletedAt = "2026-10-06T19:00:00Z"
        )

        val updateResult = profile.updateDetails(
            fullName = "Updated Name",
            major = "Biology",
            university = "CSULB",
            bio = "New bio"
        )

        assertTrue(updateResult is Result.Error)
        assertEquals("Cannot update a deleted profile.", updateResult.exception.message)
    }
}
