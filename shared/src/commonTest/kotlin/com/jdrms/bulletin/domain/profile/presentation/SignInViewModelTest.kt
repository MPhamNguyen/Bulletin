package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.domain.profile.application.SessionState
import com.jdrms.bulletin.domain.profile.application.SignInUser
import com.jdrms.bulletin.domain.profile.application.VerifyStudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.infrastructure.repository.AuthSessionRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {
    @Test
    fun signInUsesTheSharedSessionAndKeepsFailuresInline() = profileTest {
        val profiles = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val auth = InMemoryAuthRepository(profiles, testVerificationCode = "123456")
        val session = AuthSessionRepository(auth)
        val vm = SignInViewModel(SignInUser(auth), session)
        vm.signIn("bad", "password123")
        advanceUntilIdle()
        assertEquals("Invalid email address format.", vm.uiState.value.errorMessage)

        val email = StudentEmail("student@school.edu")
        auth.register(email, "password123", "Test Student")
        VerifyStudentEmail(auth)(email, "123456")
        auth.signOut()
        vm.signIn(email.value, "wrong")
        advanceUntilIdle()
        assertEquals(true, vm.uiState.value.errorMessage != null)
        vm.clearError()
        assertNull(vm.uiState.value.errorMessage)
        vm.signIn(email.value, "password123")
        advanceUntilIdle()
        assertIs<SessionState.Authenticated>(session.state.value)
    }
}
