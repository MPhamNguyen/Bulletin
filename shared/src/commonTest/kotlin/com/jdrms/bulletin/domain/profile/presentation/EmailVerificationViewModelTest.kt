package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.AuthenticateUser
import com.jdrms.bulletin.domain.profile.application.ManageProfile
import com.jdrms.bulletin.domain.profile.application.ResendVerificationCode
import com.jdrms.bulletin.domain.profile.application.RestoreAuthenticatedProfile
import com.jdrms.bulletin.domain.profile.application.SignOutUser
import com.jdrms.bulletin.domain.profile.application.SoftDeleteProfile
import com.jdrms.bulletin.domain.profile.application.SubmitStudentReview
import com.jdrms.bulletin.domain.profile.application.UpdateStudentProfile
import com.jdrms.bulletin.domain.profile.application.VerifyStudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import com.jdrms.bulletin.domain.profile.domain.repository.ProfileRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EmailVerificationViewModelTest {
    @Test
    fun failedVerificationRetainsPendingEmailAndSuccessAuthenticates() = withViewModel { viewModel ->
        viewModel.createAccount("Student", "Name", EMAIL, "password123")
        assertTrue(viewModel.uiState.value.isLoading)
        viewModel.createAccount("Duplicate", "Name", EMAIL, "password123")
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.pendingRegistration)
        assertFalse(viewModel.uiState.value.isAccountCreated)
        assertNull(viewModel.uiState.value.profile)
        assertEquals(AuthSessionState.UNAUTHENTICATED, viewModel.uiState.value.authSessionState)

        viewModel.verifyEmail("other@example.com", "123456")
        assertNotNull(viewModel.uiState.value.errorMessage)
        viewModel.verifyEmail(EMAIL, "not-a-code")
        advanceUntilIdle()
        assertEquals("Enter the 6-digit code from your email.", viewModel.uiState.value.errorMessage)
        viewModel.verifyEmail(EMAIL, "999999")
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isAccountCreated)
        assertNotNull(viewModel.uiState.value.pendingRegistration)

        viewModel.verifyEmail(EMAIL, "123456")
        viewModel.verifyEmail(EMAIL, "123456")
        runCurrent()
        assertTrue(viewModel.uiState.value.isAccountCreated)
        assertNull(viewModel.uiState.value.pendingRegistration)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(AuthSessionState.AUTHENTICATED, viewModel.uiState.value.authSessionState)
        assertEquals("Student Name", viewModel.uiState.value.profile?.fullName)
        advanceUntilIdle()
    }

    @Test
    fun resumesPendingVerificationByResendingWithoutPassword() = withViewModel { viewModel ->
        viewModel.createAccount("Student", "Name", EMAIL, "password123")
        advanceUntilIdle()
        viewModel.resetRegistration()
        assertNull(viewModel.uiState.value.pendingRegistration)
        viewModel.verifyEmail(EMAIL, "123456")
        assertFalse(viewModel.uiState.value.isAccountCreated)
        viewModel.resendEmailCode("invalid")
        assertNotNull(viewModel.uiState.value.errorMessage)
        viewModel.resendEmailCode(EMAIL)
        viewModel.resendEmailCode(EMAIL)
        assertTrue(viewModel.uiState.value.isLoading)
        advanceUntilIdle()
        assertEquals(StudentEmail(EMAIL), viewModel.uiState.value.pendingRegistration?.email)
        assertNotNull(viewModel.uiState.value.successMessage)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(AuthSessionState.UNAUTHENTICATED, viewModel.uiState.value.authSessionState)
        viewModel.verifyEmail(EMAIL, "123456")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isAccountCreated)
    }

    @Test
    fun resendFailureRetainsPendingRegistrationAndClearsLoading() = withViewModel(failResend = true) { viewModel ->
        viewModel.createAccount("Student", "Name", EMAIL, "password123")
        advanceUntilIdle()
        viewModel.resendEmailCode(EMAIL)
        advanceUntilIdle()
        assertEquals("Please wait before requesting another code.", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.pendingRegistration)
        assertFalse(viewModel.uiState.value.isAccountCreated)
    }

    @Test
    fun acceptedCodeMovesToProfileRecoveryAndRetryCompletesRegistration() = withViewModel(
        failInitialProfileSave = true
    ) { viewModel ->
        viewModel.createAccount("Student", "Name", EMAIL, "password123")
        advanceUntilIdle()

        viewModel.verifyEmail(EMAIL, "123456")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.pendingRegistration)
        assertEquals(StudentEmail(EMAIL), viewModel.uiState.value.verifiedEmailAwaitingProfile)
        assertFalse(viewModel.uiState.value.isAccountCreated)
        assertEquals(AuthSessionState.UNAUTHENTICATED, viewModel.uiState.value.authSessionState)

        viewModel.verifyEmail(EMAIL, "123456")
        viewModel.resendEmailCode(EMAIL)
        assertNotNull(viewModel.uiState.value.verifiedEmailAwaitingProfile)

        viewModel.retryVerifiedProfile()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.verifiedEmailAwaitingProfile)
        assertTrue(viewModel.uiState.value.isAccountCreated)
        assertEquals(AuthSessionState.AUTHENTICATED, viewModel.uiState.value.authSessionState)
    }

    @Test
    fun registrationFailureDoesNotCreateSessionOrPendingState() = withViewModel { viewModel ->
        viewModel.createAccount("Student", "Name", EMAIL, "password123")
        advanceUntilIdle()
        viewModel.resetRegistration()
        viewModel.createAccount("Student", "Name", EMAIL, "password123")
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.pendingRegistration)
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isAccountCreated)
        assertEquals(AuthSessionState.UNAUTHENTICATED, viewModel.uiState.value.authSessionState)
    }

    private fun withViewModel(
        failResend: Boolean = false,
        failInitialProfileSave: Boolean = false,
        block: suspend TestScope.(ProfileViewModel) -> Unit
    ) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val storedProfiles = InMemoryProfileRepository(initialProfiles = emptyMap())
            var shouldFailSave = failInitialProfileSave
            val profiles = object : ProfileRepository by storedProfiles {
                override suspend fun updateProfile(profile: StudentProfile): Result<StudentProfile> {
                    return if (shouldFailSave) {
                        shouldFailSave = false
                        Result.Error(IllegalStateException("Profile temporarily unavailable"))
                    } else {
                        storedProfiles.updateProfile(profile)
                    }
                }
            }
            val delegate = InMemoryAuthRepository(profiles, testVerificationCode = "123456")
            val auth = object : AuthRepository by delegate {
                override suspend fun resendVerificationCode(email: StudentEmail): Result<Unit> {
                    return if (failResend) {
                        Result.Error(IllegalStateException("Please wait before requesting another code."))
                    } else {
                        delegate.resendVerificationCode(email)
                    }
                }
            }
            val viewModel = ProfileViewModel(
                ProfileViewModelDependencies(
                    authenticateUser = AuthenticateUser(auth),
                    restoreAuthenticatedProfile = RestoreAuthenticatedProfile(auth),
                    signOutUser = SignOutUser(auth),
                    verifyStudentEmail = VerifyStudentEmail(auth),
                    resendVerificationCode = ResendVerificationCode(auth),
                    manageProfile = ManageProfile(profiles),
                    updateStudentProfile = UpdateStudentProfile(profiles),
                    submitStudentReview = SubmitStudentReview(profiles),
                    softDeleteProfile = SoftDeleteProfile(profiles, SignOutUser(auth))
                )
            )
            advanceUntilIdle()
            block(viewModel)
        } finally {
            Dispatchers.resetMain()
        }
    }

    companion object {
        private const val EMAIL = "student@school.edu"
    }
}
