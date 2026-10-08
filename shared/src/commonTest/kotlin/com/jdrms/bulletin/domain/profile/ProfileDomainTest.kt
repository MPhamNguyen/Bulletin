package com.jdrms.bulletin.domain.profile

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.ProfilePhoto
import com.jdrms.bulletin.domain.profile.domain.model.ProfilePhotoUrl
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.service.ProfileValidationPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileDomainTestPart1 {

    private val policy = ProfileValidationPolicy()

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
}
