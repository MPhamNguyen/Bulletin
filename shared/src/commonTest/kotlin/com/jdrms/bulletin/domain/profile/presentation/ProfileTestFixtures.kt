package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.domain.profile.application.VerifyStudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.infrastructure.repository.AuthSessionRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

internal class ProfileFixture {
    val profiles = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
    val auth = InMemoryAuthRepository(profiles, testVerificationCode = "123456")
    val session = AuthSessionRepository(auth)

    suspend fun authenticate(): StudentProfile {
        val email = StudentEmail("student@school.edu")
        auth.register(email, "password123", "Test Student")
        VerifyStudentEmail(auth)(email, "123456")
        return session.restore().getOrThrow()!!
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun profileTest(block: suspend TestScope.() -> Unit) = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    try {
        block()
    } finally {
        Dispatchers.resetMain()
    }
}
