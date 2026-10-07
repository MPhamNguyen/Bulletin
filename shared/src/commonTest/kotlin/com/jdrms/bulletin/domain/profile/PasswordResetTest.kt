package com.jdrms.bulletin.domain.profile

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.AuthenticateUser
import com.jdrms.bulletin.domain.profile.application.ManageProfile
import com.jdrms.bulletin.domain.profile.application.RequestPasswordReset
import com.jdrms.bulletin.domain.profile.application.ResendVerificationCode
import com.jdrms.bulletin.domain.profile.application.RestoreAuthenticatedProfile
import com.jdrms.bulletin.domain.profile.application.SignOutUser
import com.jdrms.bulletin.domain.profile.application.SoftDeleteProfile
import com.jdrms.bulletin.domain.profile.application.SubmitStudentReview
import com.jdrms.bulletin.domain.profile.application.UpdatePassword
import com.jdrms.bulletin.domain.profile.application.UpdateStudentProfile
import com.jdrms.bulletin.domain.profile.application.VerifyPasswordResetCode
import com.jdrms.bulletin.domain.profile.application.VerifyStudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.EmailVerificationCode
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import com.jdrms.bulletin.domain.profile.domain.service.ProfileValidationPolicy
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import com.jdrms.bulletin.domain.profile.presentation.AuthSessionState
import com.jdrms.bulletin.domain.profile.presentation.PasswordRecoveryStage
import com.jdrms.bulletin.domain.profile.presentation.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PasswordResetTest {
    private companion object {
        const val TEST_CONFIRMATION_CODE = "123456"
    }

    private val email = StudentEmail("reset@student.csulb.edu")
    private val policy = ProfileValidationPolicy()

    @Test
    fun inMemoryRepositoryChangesCredentialAfterVerifiedCode() = runTest {
        val profileRepository = InMemoryProfileRepository()
        val authRepository = InMemoryAuthRepository(
            profileRepository = profileRepository,
            testVerificationCode = TEST_CONFIRMATION_CODE
        )
        authRepository.register(email, "oldPassword", "Reset Student")
        authRepository.verifyEmail(email, testEmailVerificationCode())
        authRepository.signOut()

        assertTrue(authRepository.requestPasswordReset(email).isSuccess())
        assertTrue(authRepository.verifyPasswordResetCode(email, "wrong").isError())
        assertTrue(
            authRepository.verifyPasswordResetCode(email, TEST_CONFIRMATION_CODE).isSuccess()
        )
        assertTrue(authRepository.updatePassword("newPassword").isSuccess())

        val loginResult = authRepository.login(email, "newPassword")
        assertTrue(loginResult.isSuccess())
        assertTrue(authRepository.login(email, "oldPassword").isError())
    }

    @Test
    fun resetWorkflowMovesThroughCodeAndPasswordStages() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val profileRepository = InMemoryProfileRepository()
            val authRepository = RecordingSignOutAuthRepository(
                InMemoryAuthRepository(
                    profileRepository = profileRepository,
                    testVerificationCode = TEST_CONFIRMATION_CODE
                )
            )
            authRepository.register(email, "oldPassword", "Reset Student")
            authRepository.verifyEmail(email, testEmailVerificationCode())
            authRepository.signOut()
            val viewModel = ProfileViewModel(
                authenticateUser = AuthenticateUser(authRepository, policy),
                restoreAuthenticatedProfile = RestoreAuthenticatedProfile(authRepository),
                signOutUser = SignOutUser(authRepository),
                verifyStudentEmail = VerifyStudentEmail(authRepository),
                resendVerificationCode = ResendVerificationCode(authRepository),
                requestPasswordReset = RequestPasswordReset(authRepository, policy),
                verifyPasswordResetCode = VerifyPasswordResetCode(authRepository),
                updatePassword = UpdatePassword(authRepository, policy),
                manageProfile = ManageProfile(profileRepository),
                updateStudentProfile = UpdateStudentProfile(profileRepository),
                submitStudentReview = SubmitStudentReview(profileRepository, policy),
                softDeleteProfile = SoftDeleteProfile(profileRepository, SignOutUser(authRepository))
            )
            advanceUntilIdle()
            assertEquals(AuthSessionState.UNAUTHENTICATED, viewModel.uiState.value.authSessionState)

            viewModel.beginPasswordReset()
            viewModel.requestPasswordReset(email.value)
            advanceUntilIdle()
            assertEquals(PasswordRecoveryStage.ENTER_CODE, viewModel.uiState.value.passwordRecoveryStage)

            viewModel.verifyPasswordResetCode(TEST_CONFIRMATION_CODE)
            advanceUntilIdle()
            assertEquals(PasswordRecoveryStage.CHANGE_PASSWORD, viewModel.uiState.value.passwordRecoveryStage)

            var completed = false
            viewModel.updatePassword("newPassword", "newPassword") { completed = true }
            advanceUntilIdle()
            assertTrue(completed)
            assertEquals(PasswordRecoveryStage.NONE, viewModel.uiState.value.passwordRecoveryStage)
            assertEquals(2, authRepository.signOutCalls)
            assertTrue(authRepository.login(email, "newPassword").isSuccess())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun invalidPasswordConfirmationAndUnknownEmailAreRejected() = runTest {
        val profileRepository = InMemoryProfileRepository()
        val authRepository = InMemoryAuthRepository(
            profileRepository = profileRepository,
            testVerificationCode = TEST_CONFIRMATION_CODE
        )
        val request = RequestPasswordReset(authRepository, policy)
        val update = UpdatePassword(authRepository, policy)

        val unknownEmail = request(StudentEmail("unknown@student.csulb.edu"))
        assertTrue(unknownEmail is Result.Error)
        assertTrue(update("short", "short") is Result.Error)
        assertTrue(update("validPassword", "differentPassword") is Result.Error)
    }

    @Test
    fun inMemoryPasswordResetIsDisabledWithoutAnExplicitTestCode() = runTest {
        val authRepository = InMemoryAuthRepository(InMemoryProfileRepository())

        assertTrue(
            authRepository.requestPasswordReset(email) is Result.Error
        )
    }

    @Test
    fun failedCodeAndPasswordUpdateKeepTheRecoveryWorkflowAtItsCurrentStage() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val profileRepository = InMemoryProfileRepository()
            val authRepository = InMemoryAuthRepository(
                profileRepository = profileRepository,
                testVerificationCode = TEST_CONFIRMATION_CODE
            )
            authRepository.register(email, "oldPassword", "Reset Student")
            authRepository.verifyEmail(email, testEmailVerificationCode())
            authRepository.signOut()
            val viewModel = ProfileViewModel(
                authenticateUser = AuthenticateUser(authRepository, policy),
                restoreAuthenticatedProfile = RestoreAuthenticatedProfile(authRepository),
                signOutUser = SignOutUser(authRepository),
                verifyStudentEmail = VerifyStudentEmail(authRepository),
                resendVerificationCode = ResendVerificationCode(authRepository),
                requestPasswordReset = RequestPasswordReset(authRepository, policy),
                verifyPasswordResetCode = VerifyPasswordResetCode(authRepository),
                updatePassword = UpdatePassword(authRepository, policy),
                manageProfile = ManageProfile(profileRepository),
                updateStudentProfile = UpdateStudentProfile(profileRepository),
                submitStudentReview = SubmitStudentReview(profileRepository, policy),
                softDeleteProfile = SoftDeleteProfile(profileRepository, SignOutUser(authRepository))
            )

            viewModel.beginPasswordReset()
            viewModel.requestPasswordReset(email.value)
            advanceUntilIdle()
            viewModel.verifyPasswordResetCode("000000")
            advanceUntilIdle()

            assertEquals(PasswordRecoveryStage.ENTER_CODE, viewModel.uiState.value.passwordRecoveryStage)
            assertTrue(viewModel.uiState.value.errorMessage != null)

            viewModel.verifyPasswordResetCode(TEST_CONFIRMATION_CODE)
            advanceUntilIdle()
            viewModel.updatePassword("newPassword", "differentPassword")
            advanceUntilIdle()

            assertEquals(PasswordRecoveryStage.CHANGE_PASSWORD, viewModel.uiState.value.passwordRecoveryStage)
            assertTrue(viewModel.uiState.value.errorMessage != null)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun applicationPassesProviderGeneratedNumericCodeToRepository() = runTest {
        val repository = RecordingPasswordResetRepository()
        val verify = VerifyPasswordResetCode(repository)

        assertTrue(verify(email, "654321").isSuccess())
        assertEquals("654321", repository.receivedCode)
        assertTrue(verify(email, "not-a-code").isError())
    }

    private class RecordingPasswordResetRepository : AuthRepository by InMemoryAuthRepository(
        InMemoryProfileRepository()
    ) {
        var receivedCode: String? = null

        override suspend fun verifyPasswordResetCode(email: StudentEmail, code: String): Result<Unit> {
            receivedCode = code
            return Result.Success(Unit)
        }
    }

    private class RecordingSignOutAuthRepository(
        private val delegate: AuthRepository
    ) : AuthRepository by delegate {
        var signOutCalls: Int = 0

        override suspend fun signOut(): Result<Unit> {
            signOutCalls += 1
            return delegate.signOut()
        }
    }

    private fun testEmailVerificationCode(): EmailVerificationCode {
        return when (val result = EmailVerificationCode.parse(TEST_CONFIRMATION_CODE)) {
            is Result.Success -> result.data
            is Result.Error -> error("Test verification code should be valid.")
        }
    }
}
