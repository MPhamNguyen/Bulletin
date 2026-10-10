package com.jdrms.bulletin.domain.profile

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.RegisterStudent
import com.jdrms.bulletin.domain.profile.application.RestoreAuthenticatedProfile
import com.jdrms.bulletin.domain.profile.application.SignOutUser
import com.jdrms.bulletin.domain.profile.application.SoftDeleteProfile
import com.jdrms.bulletin.domain.profile.application.UpdateStudentProfile
import com.jdrms.bulletin.domain.profile.application.UploadProfilePhoto
import com.jdrms.bulletin.domain.profile.application.VerifyStudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.EmailVerificationOutcome
import com.jdrms.bulletin.domain.profile.domain.model.ProfilePhoto
import com.jdrms.bulletin.domain.profile.domain.model.ProfilePhotoUrl
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
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

        val result = UploadProfilePhoto(photos, profiles)(profile, jpegBytes(), "image/jpeg")

        assertTrue(result is Result.Success)
        assertEquals(4, uploadedPhoto?.sizeBytes)
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
    fun testRegisterStudentValidatesAndRequestsRegistration() = runTest {
        val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val authRepo = InMemoryAuthRepository(profileRepo, testVerificationCode = "123456")
        val registerStudent = RegisterStudent(authRepo, policy)

        val registerResult = registerStudent(
            firstName = "First",
            lastName = "Last",
            email = "student1@school.edu",
            password = "validPassword123",
        )
        assertTrue(registerResult.isSuccess())

        // Invalid registration fails in use case
        val shortPasswordResult = registerStudent(
            firstName = "Short",
            lastName = "Pass",
            email = "student2@school.edu",
            password = "123",
        )
        assertTrue(shortPasswordResult.isError())

        val nonEduResult = registerStudent(
            firstName = "No",
            lastName = "University",
            email = "student3@example.com",
            password = "validPassword123",
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
        val registerStudent = RegisterStudent(authRepo, policy)
        val restoreAuthenticatedProfile = RestoreAuthenticatedProfile(authRepo)
        val signOutUser = SignOutUser(authRepo)

        assertNull((restoreAuthenticatedProfile() as Result.Success).data)

        val registered = registerStudent(
            firstName = "Student",
            lastName = "Name",
            email = "student@school.edu",
            password = "validPassword123",
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

    private fun jpegBytes(): ByteArray = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 0x01)

    @Test
    fun testSoftDeleteProfilePersistsTimestampAndSignsOut() = runTest {
        val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val authRepo = InMemoryAuthRepository(profileRepo, testVerificationCode = "123456")
        val signOutUser = SignOutUser(authRepo)

        val profile = StudentProfile(
            id = UserId("student_1"),
            email = StudentEmail("student@school.edu"),
            fullName = "Student Name"
        )
        profileRepo.updateProfile(profile)

        val fixedTimestamp = "2026-10-06T19:00:00Z"
        val softDeleteProfile = SoftDeleteProfile(
            profileRepository = profileRepo,
            signOutUser = signOutUser,
            nowTimestamp = { fixedTimestamp }
        )

        val result = softDeleteProfile(profile)
        assertTrue(result is Result.Success)
        assertEquals(fixedTimestamp, result.data.deletedAt)
        assertTrue(result.data.isDeleted)

        val persisted = profileRepo.getProfile(profile.id)
        assertTrue(persisted is Result.Success)
        assertEquals(fixedTimestamp, persisted.data?.deletedAt)

        val restoredUser = RestoreAuthenticatedProfile(authRepo)()
        assertTrue(restoredUser is Result.Success)
        assertNull(restoredUser.data)
    }

    @Test
    fun testSoftDeleteProfileReportsSignOutFailureAfterPersistingDeletion() = runTest {
        val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val authRepo = InMemoryAuthRepository(profileRepo)
        val profile = StudentProfile(
            id = UserId("student_1"),
            email = StudentEmail("student@school.edu"),
            fullName = "Student Name"
        )
        profileRepo.updateProfile(profile)

        val softDeleteProfile = SoftDeleteProfile(
            profileRepository = profileRepo,
            signOutUser = SignOutUser(FailingSignOutAuthRepository(authRepo)),
            nowTimestamp = { "2026-10-06T19:00:00Z" }
        )

        val result = softDeleteProfile(profile)

        assertTrue(result is Result.Error)
        assertEquals("Sign-out cleanup unavailable", result.exception.message)
        val persisted = profileRepo.getProfile(profile.id)
        assertTrue(persisted is Result.Success)
        assertEquals("2026-10-06T19:00:00Z", persisted.data?.deletedAt)
    }

    @Test
    fun testSoftDeleteProfileFailsOnAlreadyDeletedProfile() = runTest {
        val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val authRepo = InMemoryAuthRepository(profileRepo)
        val signOutUser = SignOutUser(authRepo)

        val alreadyDeleted = StudentProfile(
            id = UserId("student_1"),
            email = StudentEmail("student@school.edu"),
            fullName = "Student Name",
            deletedAt = "2026-10-06T18:00:00Z"
        )

        val softDeleteProfile = SoftDeleteProfile(profileRepo, signOutUser)
        val result = softDeleteProfile(alreadyDeleted)
        assertTrue(result is Result.Error)
        assertEquals("Profile is already deleted.", result.exception.message)
    }
}

private class FailingSignOutAuthRepository(
    private val delegate: AuthRepository
) : AuthRepository by delegate {
    override suspend fun signOut(): Result<Unit> {
        return Result.Error(IllegalStateException("Sign-out cleanup unavailable"))
    }
}
