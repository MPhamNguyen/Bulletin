package com.jdrms.bulletin.domain.profile

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.AuthenticateUser
import com.jdrms.bulletin.domain.profile.application.DeleteStudentAccount
import com.jdrms.bulletin.domain.profile.application.RestoreAuthenticatedProfile
import com.jdrms.bulletin.domain.profile.application.SignOutUser
import com.jdrms.bulletin.domain.profile.application.UpdateStudentProfile
import com.jdrms.bulletin.domain.profile.application.VerifyStudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.EmailVerificationOutcome
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.service.ProfileValidationPolicy
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileApplicationTest {

    private val policy = ProfileValidationPolicy()

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

    @Test
    fun testDeleteStudentAccountDeletesProfileAndSignsOut() = runTest {
        val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val authRepo = InMemoryAuthRepository(profileRepo)
        val authenticateUser = AuthenticateUser(authRepo, policy)
        val restoreAuthenticatedProfile = RestoreAuthenticatedProfile(authRepo)
        val fixedTimestamp = 1_700_000_000_000L
        val deleteStudentAccount = DeleteStudentAccount(
            profileRepository = profileRepo,
            authRepository = authRepo,
            nowMillis = { fixedTimestamp }
        )

        val registered = authenticateUser.register(
            email = StudentEmail("delete_me@example.com"),
            password = "validPassword123",
            fullName = "To Be Deleted"
        )
        assertTrue(registered is Result.Success)
        val userId = registered.data.id

        // Verify active session before deletion
        val currentProfile = restoreAuthenticatedProfile()
        assertTrue(currentProfile is Result.Success)
        assertNotNull(currentProfile.data)

        // Delete account
        val deleteResult = deleteStudentAccount(userId)
        assertTrue(deleteResult.isSuccess())

        // Verify session signed out
        val sessionAfterDelete = restoreAuthenticatedProfile()
        assertTrue(sessionAfterDelete is Result.Success)
        assertNull(sessionAfterDelete.data)

        // Verify repository profile is soft deleted with timestamp
        val storedProfile = profileRepo.getProfile(userId)
        assertTrue(storedProfile is Result.Success)
        val profileData = storedProfile.data
        assertNotNull(profileData)
        assertTrue(profileData.isDeleted)
        assertEquals(fixedTimestamp, profileData.deleteAtMillis)
    }

    @Test
    fun testDeleteStudentAccountFailsForNonexistentProfile() = runTest {
        val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val authRepo = InMemoryAuthRepository(profileRepo)
        val deleteStudentAccount = DeleteStudentAccount(profileRepo, authRepo)

        val deleteResult = deleteStudentAccount(UserId("nonexistent_user"))
        assertTrue(deleteResult.isError())
    }

    @Test
    fun testAuthenticateUserLoginFailsForSoftDeletedAccount() = runTest {
        val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val authRepo = InMemoryAuthRepository(profileRepo)
        val authenticateUser = AuthenticateUser(authRepo, policy)

        val email = StudentEmail("deleted_user@example.com")
        val registerResult = authenticateUser.register(
            email = email,
            password = "validPassword123",
            fullName = "Deleted User"
        )
        assertTrue(registerResult is Result.Success)

        // Soft delete the profile
        val deleteResult = profileRepo.deleteProfile(registerResult.data.id, 1_700_000_000_000L)
        assertTrue(deleteResult.isSuccess())

        // Attempting to login fails
        val loginResult = authenticateUser.login(email, "validPassword123")
        assertTrue(loginResult is Result.Error)
        assertEquals("Account has been deleted.", loginResult.exception.message)
    }
}
