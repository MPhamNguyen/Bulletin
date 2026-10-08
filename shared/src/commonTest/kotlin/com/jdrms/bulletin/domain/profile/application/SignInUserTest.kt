package com.jdrms.bulletin.domain.profile.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SignInUserTest {
    @Test
    fun signInValidatesBeforeCallingAuthenticationAndSucceedsWithVerifiedCredentials() = runTest {
        val repository = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val auth = InMemoryAuthRepository(repository, testVerificationCode = "123456")
        val signIn = SignInUser(auth)
        assertIs<Result.Error>(signIn("bad", "password123"))
        val pending = RegisterStudent(auth)("Test", "Student", "student@school.edu", "password123").getOrThrow()
        VerifyStudentEmail(auth)(pending.email, "123456")
        auth.signOut()
        assertIs<Result.Error>(signIn(pending.email.value, "wrong"))
        assertEquals("Test Student", signIn(pending.email.value, "password123").getOrThrow().fullName)
    }
}
