package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.domain.profile.application.SessionState
import com.jdrms.bulletin.domain.profile.application.SignInExternally
import com.jdrms.bulletin.domain.profile.application.SignInUser
import com.jdrms.bulletin.domain.profile.domain.model.ExternalSignInFailure
import com.jdrms.bulletin.domain.profile.domain.model.ExternalSignInOutcome
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.AuthSessionRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryAuthRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ExternalSignInViewModelTest {
    @Test
    fun disablesBothSignInMethodsDuringBrowserFlowAndIgnoresRepeatedTaps() = profileTest {
        val completion = CompletableDeferred<ExternalSignInOutcome>()
        var calls = 0
        val auth = object : AuthRepository by InMemoryAuthRepository() {
            override suspend fun signInExternally(): ExternalSignInOutcome {
                calls++
                return completion.await()
            }
        }
        val session = AuthSessionRepository(auth)
        val viewModel = SignInViewModel(SignInUser(auth), session, SignInExternally(auth))
        viewModel.signInWithMicrosoft()
        viewModel.signInWithMicrosoft()
        viewModel.signIn("invalid", "ignored")
        runCurrent()
        assertEquals(1, calls)
        assertEquals(true, viewModel.uiState.value.isLoading)
        assertEquals(ExternalSignInStage.InProgress, viewModel.uiState.value.externalStage)
        completion.complete(ExternalSignInOutcome.SignedIn(profile))
        advanceUntilIdle()
        assertEquals(profile, assertIs<SessionState.Authenticated>(session.state.value).profile)
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun typedRejectionsShowFixedRecoverableMessages() = profileTest {
        for (reason in ExternalSignInFailure.entries) {
            val auth = object : AuthRepository by InMemoryAuthRepository() {
                override suspend fun signInExternally() = ExternalSignInOutcome.Failed(reason)
            }
            val session = AuthSessionRepository(auth)
            val viewModel = SignInViewModel(SignInUser(auth), session, SignInExternally(auth))
            viewModel.signInWithMicrosoft()
            advanceUntilIdle()
            assertEquals(ExternalSignInStage.Error, viewModel.uiState.value.externalStage)
            assertEquals(reason.userMessage(), viewModel.uiState.value.errorMessage)
            assertEquals(false, viewModel.uiState.value.isLoading)
            viewModel.clearError()
            assertNull(viewModel.uiState.value.errorMessage)
        }
    }

    companion object {
        private val profile = StudentProfile(UserId("student"), StudentEmail("student@outlook.com"), "Student")
    }
}
