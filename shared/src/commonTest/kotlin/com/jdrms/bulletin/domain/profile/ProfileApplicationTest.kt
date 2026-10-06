package com.jdrms.bulletin.domain.profile

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.AuthenticateUser
import com.jdrms.bulletin.domain.profile.application.RestoreAuthenticatedProfile
import com.jdrms.bulletin.domain.profile.application.SignOutUser
import com.jdrms.bulletin.domain.profile.application.UpdateStudentProfile
import com.jdrms.bulletin.domain.profile.application.UploadProfilePhoto
import com.jdrms.bulletin.domain.profile.application.VerifyStudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.EmailVerificationOutcome
import com.jdrms.bulletin.domain.profile.domain.model.ProfilePhoto
import com.jdrms.bulletin.domain.profile.domain.model.ProfilePhotoUrl
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.ProfilePhotoRepository
import com.jdrms.bulletin.domain.profile.domain.service.ProfileValidationPolicy
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileApplicationTest {

    private val policy = ProfileValidationPolicy()

    @Test
    fun uploadProfilePhotoUploadsAndPersistsTheReturnedUrl() = runTest {
        val profiles = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val profile = StudentProfile(UserId("student_1"), StudentEmail("student@example.com"), "Student")
        profiles.updateProfile(profile)
        var uploadedPhoto: ProfilePhoto? = null
        val photos = object : ProfilePhotoRepository {
            override suspend fun upload(userId: UserId, photo: ProfilePhoto): Result<ProfilePhotoUrl> {
                assertEquals(profile.id, userId)
                uploadedPhoto = photo
                return Result.Success(ProfilePhotoUrl("https://example.com/pfp/student_1/avatar.jpg"))
            }
        }

        val result = UploadProfilePhoto(photos, profiles)(profile, byteArrayOf(1, 2), "image/jpeg")

        assertTrue(result is Result.Success)
        assertEquals(2, uploadedPhoto?.sizeBytes)
        assertEquals(result.data.avatarUrl, (profiles.getProfile(profile.id) as Result.Success).data?.avatarUrl)
    }

    @Test
    fun uploadProfilePhotoStopsBeforeStorageWhenTheImageIsInvalid() = runTest {
        val profiles = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val profile = StudentProfile(UserId("student_1"), StudentEmail("student@example.com"), "Student")
        var uploadCalled = false
        val photos = object : ProfilePhotoRepository {
            override suspend fun upload(userId: UserId, photo: ProfilePhoto): Result<ProfilePhotoUrl> {
                uploadCalled = true
                return Result.Success(ProfilePhotoUrl("https://example.com/avatar"))
            }
        }

        val result = UploadProfilePhoto(photos, profiles)(profile, byteArrayOf(1), "image/gif")

        assertTrue(result is Result.Error)
        assertFalse(uploadCalled)
    }

    @Test
    fun testUpdateStudentProfilePersistsValidDetails() = runTest {
        val repository = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val profile = StudentProfile(
            id = UserId("student_1"),
            email = StudentEmail("student@example.com"),
            fullName = "Original Name"
        )
        repository.updateProfile(profile)

        val result = UpdateStudentProfile(repository)(
            profile = profile,
            fullName = "John Doe",
            major = "Computer Science",
            university = "CSULB",
            bio = "Student bio"
        )

        assertTrue(result is Result.Success)
        assertEquals("Computer Science", result.data.major)
        val persisted = repository.getProfile(profile.id)
        assertTrue(persisted is Result.Success)
        assertEquals(result.data, persisted.data?.copy(reputation = null))
    }

    @Test
    fun testUpdateStudentProfileDoesNotPersistInvalidDetails() = runTest {
        val repository = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val profile = StudentProfile(
            id = UserId("student_1"),
            email = StudentEmail("student@example.com"),
            fullName = "Original Name"
        )
        repository.updateProfile(profile)

        val result = UpdateStudentProfile(repository)(
            profile = profile,
            fullName = " ",
            major = "Computer Science",
            university = "CSULB",
            bio = "Student bio"
        )

        assertTrue(result is Result.Error)
        val persisted = repository.getProfile(profile.id)
        assertTrue(persisted is Result.Success)
        assertEquals("Original Name", persisted.data?.fullName)
    }

    @Test
    fun testAuthenticateUserUseCaseRegisterAndLogin() = runTest {
        val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val authRepo = InMemoryAuthRepository(profileRepo, testVerificationCode = "123456")
        val authenticateUser = AuthenticateUser(authRepo, policy)

        val email = StudentEmail("student1@school.edu")
        val registerResult = authenticateUser.register(
            email = email,
            password = "validPassword123",
            fullName = "First Last"
        )
        assertTrue(registerResult.isSuccess())

        // Invalid registration fails in use case
        val shortPasswordResult = authenticateUser.register(
            email = StudentEmail("student2@school.edu"),
            password = "123",
            fullName = "Short Pass"
        )
        assertTrue(shortPasswordResult.isError())

        val nonEduResult = authenticateUser.register(
            email = StudentEmail("student3@example.com"),
            password = "validPassword123",
            fullName = "No University Email"
        )
        assertTrue(nonEduResult.isError())
        assertEquals(
            "Bulletin requires a valid .edu university email.",
            (nonEduResult as Result.Error).exception.message
        )
    }

    @Test
    fun testAuthenticatedSessionCanBeRestoredAndSignedOut() = runTest {
        val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val authRepo = InMemoryAuthRepository(profileRepo, testVerificationCode = "123456")
        val authenticateUser = AuthenticateUser(authRepo, policy)
        val restoreAuthenticatedProfile = RestoreAuthenticatedProfile(authRepo)
        val signOutUser = SignOutUser(authRepo)

        assertNull((restoreAuthenticatedProfile() as Result.Success).data)

        val registered = authenticateUser.register(
            email = StudentEmail("student@school.edu"),
            password = "validPassword123",
            fullName = "Student Name"
        )
        assertTrue(registered is Result.Success)
        assertNull((restoreAuthenticatedProfile() as Result.Success).data)
        val verified = VerifyStudentEmail(authRepo)(registered.data.email, "123456")
        assertTrue(verified is Result.Success)
        val profile = assertIs<EmailVerificationOutcome.ProfileAvailable>(verified.data).profile
        assertEquals(profile, (restoreAuthenticatedProfile() as Result.Success).data)

        assertTrue(signOutUser() is Result.Success)
        assertNull((restoreAuthenticatedProfile() as Result.Success).data)
    }
}
