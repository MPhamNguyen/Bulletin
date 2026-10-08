package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.RegisterStudent
import com.jdrms.bulletin.domain.profile.application.ResendVerificationCode
import com.jdrms.bulletin.domain.profile.application.SessionState
import com.jdrms.bulletin.domain.profile.application.VerifyStudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import com.jdrms.bulletin.domain.profile.domain.repository.ProfileRepository
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
class RegistrationViewModelTest {
    @Test
    fun registrationKeepsPendingEmailAfterInvalidCodeAndAuthenticatesAfterValidCode() = profileTest {
        val profiles = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val auth = InMemoryAuthRepository(profiles, testVerificationCode = "123456")
        val session = AuthSessionRepository(auth)
        val vm = RegistrationViewModel(
            RegisterStudent(auth),
            VerifyStudentEmail(auth),
            ResendVerificationCode(auth),
            session
        )

        vm.register("", "Student", "student@school.edu", "password123")
        advanceUntilIdle()
        assertEquals("First name is required.", vm.uiState.value.errorMessage)
        vm.register("Test", "Student", "student@school.edu", "password123")
        advanceUntilIdle()
        assertIs<RegistrationStage.Verification>(vm.uiState.value.stage)
        assertIs<SessionState.Checking>(session.state.value)

        vm.verify("000000")
        advanceUntilIdle()
        assertIs<RegistrationStage.Verification>(vm.uiState.value.stage)
        assertEquals(true, vm.uiState.value.errorMessage != null)

        vm.verify("123456")
        advanceUntilIdle()
        val complete = assertIs<RegistrationStage.Complete>(vm.uiState.value.stage)
        assertEquals("Test Student", complete.profile.fullName)
        assertEquals(complete.profile, assertIs<SessionState.Authenticated>(session.state.value).profile)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun registrationResendAndResetPreserveTheStageRules() = profileTest {
        val profiles = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val auth = InMemoryAuthRepository(profiles, testVerificationCode = "123456")
        val vm = RegistrationViewModel(
            RegisterStudent(auth),
            VerifyStudentEmail(auth),
            ResendVerificationCode(auth),
            AuthSessionRepository(auth)
        )
        vm.resendCode()
        assertIs<RegistrationStage.Form>(vm.uiState.value.stage)
        vm.register("Test", "Student", "student@school.edu", "password123")
        advanceUntilIdle()
        vm.resendCode()
        advanceUntilIdle()
        assertIs<RegistrationStage.Verification>(vm.uiState.value.stage)
        assertEquals(true, vm.uiState.value.informationMessage != null)
        vm.reset()
        assertIs<RegistrationStage.Form>(vm.uiState.value.stage)
    }

    @Test
    fun verifiedEmailCanRecoverItsProfileWithoutRepeatingVerification() = profileTest {
        val profiles = object : ProfileRepository by InMemoryProfileRepository() {
            override suspend fun updateProfile(profile: StudentProfile): Result<StudentProfile> =
                Result.Error(IllegalStateException("profile save failed"))
        }
        val auth = InMemoryAuthRepository(profiles, testVerificationCode = "123456")
        val session = AuthSessionRepository(auth)
        val vm = RegistrationViewModel(
            RegisterStudent(auth),
            VerifyStudentEmail(auth),
            ResendVerificationCode(auth),
            session
        )
        vm.register("Test", "Student", "student@school.edu", "password123")
        advanceUntilIdle()
        vm.verify("123456")
        advanceUntilIdle()
        assertIs<RegistrationStage.Recovery>(vm.uiState.value.stage)
        vm.retryProfile()
        advanceUntilIdle()
        assertIs<RegistrationStage.Complete>(vm.uiState.value.stage)
        assertIs<SessionState.Authenticated>(session.state.value)
    }

    @Test
    fun resendFailureKeepsPendingRegistration() = profileTest {
        val auth = InMemoryAuthRepository(testVerificationCode = "123456")
        val failingResend = object : AuthRepository by auth {
            override suspend fun resendVerificationCode(email: StudentEmail): Result<Unit> =
                Result.Error(IllegalStateException("resend failed"))
        }
        val vm = RegistrationViewModel(
            RegisterStudent(auth),
            VerifyStudentEmail(auth),
            ResendVerificationCode(failingResend),
            AuthSessionRepository(auth)
        )
        vm.register("Test", "Student", "student@school.edu", "password123")
        advanceUntilIdle()
        vm.resendCode()
        advanceUntilIdle()
        assertEquals("resend failed", vm.uiState.value.errorMessage)
        assertIs<RegistrationStage.Verification>(vm.uiState.value.stage)
    }
}
