package com.jdrms.bulletin.domain.profile

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.AuthenticateUser
import com.jdrms.bulletin.domain.profile.application.RestoreAuthenticatedProfile
import com.jdrms.bulletin.domain.profile.application.SignOutUser
import com.jdrms.bulletin.domain.profile.application.UpdateStudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.service.ProfileValidationPolicy
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
            fullName = "Updated Name",
            major = "Computer Science",
            university = "CSULB",
            bio = "Student bio"
        )

        assertTrue(result is Result.Success)
        val persisted = repository.getProfile(profile.id)
        assertTrue(persisted is Result.Success)
        assertEquals("Updated Name", persisted.data?.fullName)
        assertEquals("Computer Science", persisted.data?.major)
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
        val authRepo = InMemoryAuthRepository(profileRepo)
        val authenticateUser = AuthenticateUser(authRepo, policy)

        val email = StudentEmail("student1@example.com")
        val registerResult = authenticateUser.register(
            email = email,
            password = "validPassword123",
            fullName = "First Last"
        )
        assertTrue(registerResult.isSuccess())

        // Invalid registration fails in use case
        val shortPasswordResult = authenticateUser.register(
            email = StudentEmail("student2@example.com"),
            password = "123",
            fullName = "Short Pass"
        )
        assertTrue(shortPasswordResult.isError())
    }

    @Test
    fun testAuthenticatedSessionCanBeRestoredAndSignedOut() = runTest {
        val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val authRepo = InMemoryAuthRepository(profileRepo)
        val authenticateUser = AuthenticateUser(authRepo, policy)
        val restoreAuthenticatedProfile = RestoreAuthenticatedProfile(authRepo)
        val signOutUser = SignOutUser(authRepo)

        assertNull((restoreAuthenticatedProfile() as Result.Success).data)

        val registered = authenticateUser.register(
            email = StudentEmail("student@example.com"),
            password = "validPassword123",
            fullName = "Student Name"
        )
        assertTrue(registered is Result.Success)
        assertEquals(registered.data, (restoreAuthenticatedProfile() as Result.Success).data)

        assertTrue(signOutUser() is Result.Success)
        assertNull((restoreAuthenticatedProfile() as Result.Success).data)
    }
}
