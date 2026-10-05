package com.jdrms.bulletin.domain.profile

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.AuthenticateUser
import com.jdrms.bulletin.domain.profile.application.ResendVerificationCode
import com.jdrms.bulletin.domain.profile.application.VerifyStudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.EmailVerificationCode
import com.jdrms.bulletin.domain.profile.domain.model.EmailVerificationOutcome
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import com.jdrms.bulletin.domain.profile.domain.repository.ProfileRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class EmailVerificationApplicationTest {
    private val email = StudentEmail("student@school.edu")

    @Test
    fun registrationRemainsUnauthenticatedUntilCorrectCodeAndCannotReplay() = runTest {
        val repository = InMemoryAuthRepository(testVerificationCode = "012345")
        val registration = AuthenticateUser(repository).register(email, "password123", "Student Name")
        assertIs<Result.Success<*>>(registration)
        assertEquals(email, registration.getOrNull()?.email)
        assertNull((repository.getCurrentUserId() as Result.Success).data)
        assertIs<Result.Error>(repository.login(email, "password123"))
        assertIs<Result.Error>(VerifyStudentEmail(repository)(email, "111111"))
        assertIs<Result.Error>(VerifyStudentEmail(repository)(StudentEmail("other@example.com"), "012345"))
        assertNull((repository.getCurrentUser() as Result.Success).data)
        assertIs<Result.Success<Unit>>(ResendVerificationCode(repository)(email))
        val verified = assertIs<Result.Success<EmailVerificationOutcome>>(
            VerifyStudentEmail(repository)(email, "012345")
        )
        val profile = assertIs<EmailVerificationOutcome.ProfileAvailable>(verified.data).profile
        assertEquals(email, profile.email)
        assertEquals(profile, (repository.getCurrentUser() as Result.Success).data)
        assertIs<Result.Error>(VerifyStudentEmail(repository)(email, "012345"))
        repository.signOut()
        assertIs<Result.Success<StudentProfile>>(repository.login(email, "password123"))
    }

    @Test
    fun defaultInMemoryAdapterDoesNotPretendToDeliverEmail() = runTest {
        val repository = InMemoryAuthRepository()
        assertIs<Result.Error>(AuthenticateUser(repository).register(email, "password123", "Student Name"))
        assertIs<Result.Error>(ResendVerificationCode(repository)(email))
        assertIs<Result.Error>(VerifyStudentEmail(repository)(email, "123456"))
        assertNull((repository.getCurrentUser() as Result.Success).data)
    }

    @Test
    fun acceptedCodeReportsRecoveryWhenProfileSaveFails() = runTest {
        val profiles = object : ProfileRepository by InMemoryProfileRepository() {
            override suspend fun updateProfile(profile: StudentProfile): Result<StudentProfile> {
                return Result.Error(IllegalStateException("Profile save failed"))
            }
        }
        val repository = InMemoryAuthRepository(profiles, testVerificationCode = "123456")
        AuthenticateUser(repository).register(email, "password123", "Student Name")
        val result = assertIs<Result.Success<EmailVerificationOutcome>>(
            VerifyStudentEmail(repository)(email, "123456")
        )
        assertIs<EmailVerificationOutcome.ProfileRecoveryRequired>(result.data)
        assertEquals(true, (repository.getCurrentUser() as Result.Success).data?.isVerified)
        assertIs<Result.Error>(VerifyStudentEmail(repository)(email, "123456"))
    }

    @Test
    fun rejectsMalformedCodeBeforeCallingPortAndPropagatesProviderFailures() = runTest {
        var calls = 0
        val failure = Result.Error(IllegalStateException("Code expired"))
        val repository = object : AuthRepository by InMemoryAuthRepository(InMemoryProfileRepository()) {
            override suspend fun verifyEmail(
                email: StudentEmail,
                code: EmailVerificationCode
            ): Result<EmailVerificationOutcome> {
                calls++
                assertEquals("012345", code.value)
                return failure
            }

            override suspend fun resendVerificationCode(email: StudentEmail): Result<Unit> = failure
        }
        assertIs<Result.Error>(VerifyStudentEmail(repository)(email, "abc"))
        assertEquals(0, calls)
        assertEquals(failure, VerifyStudentEmail(repository)(email, " 012345 "))
        assertEquals(1, calls)
        assertEquals(failure, ResendVerificationCode(repository)(email))
    }
}
