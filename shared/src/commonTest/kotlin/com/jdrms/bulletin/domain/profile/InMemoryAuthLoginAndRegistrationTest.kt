package com.jdrms.bulletin.domain.profile

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.VerifyStudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.EmailVerificationOutcome
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InMemoryAuthLoginAndRegistrationTest {

    @Test
    fun testAuthRepositoryWithExplicitCredentials() = runTest {
        val profileRepo = InMemoryProfileRepository()
        val authRepo = InMemoryAuthRepository(
            profileRepository = profileRepo,
            initialCredentials = mapOf("dominic.alfonso@student.csulb.edu" to "password123")
        )
        val email = StudentEmail("dominic.alfonso@student.csulb.edu")
        val loginWrongPassword = authRepo.login(email, "wrongPassword")
        assertTrue(loginWrongPassword.isError())
        assertEquals(
            "Incorrect password. Please try again.",
            (loginWrongPassword as Result.Error).exception.message
        )

        val unknownEmail = StudentEmail("unknown@csulb.edu")
        val loginUnknownEmail = authRepo.login(unknownEmail, "password123")
        assertTrue(loginUnknownEmail.isError())
        assertEquals(
            "Account not found. Please check your email or create an account.",
            (loginUnknownEmail as Result.Error).exception.message
        )

        val loginCorrect = authRepo.login(email, "password123")
        assertTrue(loginCorrect.isSuccess())
    }

    @Test
    fun testAuthRepositoryWithoutCredentialsFailsByDefault() = runTest {
        val profileRepo = InMemoryProfileRepository()
        val authRepo = InMemoryAuthRepository(
            profileRepository = profileRepo,
            initialCredentials = emptyMap()
        )
        val email = StudentEmail("dominic.alfonso@student.csulb.edu")
        val loginResult = authRepo.login(email, "password123")
        assertTrue(loginResult.isError())
        assertEquals(
            "Account not found. Please check your email or create an account.",
            (loginResult as Result.Error).exception.message
        )
    }

    @Test
    fun testInMemoryAuthRepositoryRegisterAndLogin() = runTest {
        val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val authRepo = InMemoryAuthRepository(profileRepo, testVerificationCode = "123456")

        val email = StudentEmail("newuser@gmail.com")
        val registerResult = authRepo.register(
            email = email,
            password = "mypassword123",
            fullName = "New User",
            university = "CSU Long Beach"
        )
        assertTrue(registerResult is Result.Success)

        assertTrue(authRepo.login(email, "mypassword123").isError())
        assertNull((authRepo.getCurrentUser() as Result.Success).data)
        val verified = VerifyStudentEmail(authRepo)(email, "123456")
        assertTrue(verified is Result.Success)
        val createdProfile = assertIs<EmailVerificationOutcome.ProfileAvailable>(verified.data).profile
        assertEquals("New User", createdProfile.fullName)
        assertEquals(email, createdProfile.email)
        assertEquals(createdProfile, (authRepo.getCurrentUser() as Result.Success).data)

        // Login with correct password
        val loginSuccess = authRepo.login(email, "mypassword123")
        assertTrue(loginSuccess is Result.Success)
        assertEquals(createdProfile.id, loginSuccess.data.id)

        // Login with wrong password
        val loginWrongPassword = authRepo.login(email, "wrongPassword")
        assertTrue(loginWrongPassword.isError())

        // Duplicate registration fails
        val duplicateRegister = authRepo.register(
            email = email,
            password = "anotherPassword",
            fullName = "Duplicate User"
        )
        assertTrue(duplicateRegister.isError())

        assertTrue(authRepo.signOut().isSuccess())
        assertNull((authRepo.getCurrentUser() as Result.Success).data)
    }

    @Test
    fun testInMemoryAuthRepositoryRejectsLoginForDeletedProfile() = runTest {
        val repo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val authRepo = InMemoryAuthRepository(
            profileRepository = repo,
            initialCredentials = mapOf("student@school.edu" to "pass123"),
            initialSeedUserIds = mapOf("student@school.edu" to UserId("current_student"))
        )
        val profile = StudentProfile(
            id = UserId("current_student"),
            email = StudentEmail("student@school.edu"),
            fullName = "Student Name",
            deletedAt = "2026-10-06T19:00:00Z"
        )
        repo.updateProfile(profile)

        val loginResult = authRepo.login(profile.email, "pass123")
        assertTrue(loginResult is Result.Error)
        assertEquals(
            "Account not found. Please check your email or create an account.",
            loginResult.exception.message
        )
    }
}
