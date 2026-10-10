package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.SessionState
import com.jdrms.bulletin.domain.profile.application.VerifyStudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs

class AuthSessionRepositoryTest {
    @Test
    fun sessionRestoreTracksTheAuthenticationProvider() = runTest {
        val profiles = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val auth = InMemoryAuthRepository(profiles, testVerificationCode = "123456")
        val session = AuthSessionRepository(auth)
        assertIs<Result.Success<*>>(session.restore())
        assertIs<SessionState.Unauthenticated>(session.state.value)
        val email = StudentEmail("student@school.edu")
        auth.register(email, "password123", "Test Student")
        VerifyStudentEmail(auth)(email, "123456")
        session.restore()
        assertIs<SessionState.Authenticated>(session.state.value)
        auth.signOut()
        session.restore()
        assertIs<SessionState.Unauthenticated>(session.state.value)
    }

    @Test
    fun restoreFailureClearsThePublishedSession() = runTest {
        val auth = object : AuthRepository by InMemoryAuthRepository() {
            override suspend fun getCurrentUser(): Result<StudentProfile?> =
                Result.Error(IllegalStateException("session unavailable"))
        }
        val session = AuthSessionRepository(auth)
        assertIs<Result.Error>(session.restore())
        assertIs<SessionState.Unauthenticated>(session.state.value)
    }
}
