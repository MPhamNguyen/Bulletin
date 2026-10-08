package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.RestoreAuthenticatedProfile
import com.jdrms.bulletin.domain.profile.application.SignOutUser
import com.jdrms.bulletin.domain.profile.application.SoftDeleteProfile
import com.jdrms.bulletin.domain.profile.application.VerifyStudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.EmailVerificationOutcome
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.ProfileRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileViewModelTestPart4 : ProfileViewModelTestPart2() {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testHandleSubscreenBackDoesNotTriggerRootOnBackForSubscreens() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
            val authRepo = InMemoryAuthRepository(profileRepo)
            val viewModel = createProfileViewModel(authRepo, profileRepo)
            advanceUntilIdle()

            var onBackCalled = false
            val onBack = { onBackCalled = true }

            viewModel.openNotifications()
            assertEquals(ProfileSubscreen.NOTIFICATIONS, viewModel.uiState.value.activeSubscreen)
            handleSubscreenBack(ProfileSubscreen.NOTIFICATIONS, viewModel, onBack)
            assertEquals(ProfileSubscreen.SETTINGS, viewModel.uiState.value.activeSubscreen)
            assertFalse(onBackCalled)

            handleSubscreenBack(ProfileSubscreen.SETTINGS, viewModel, onBack)
            assertEquals(ProfileSubscreen.PROFILE, viewModel.uiState.value.activeSubscreen)
            assertFalse(onBackCalled)

            viewModel.openSettings()
            viewModel.openEditAccount()
            handleSubscreenBack(ProfileSubscreen.EDIT_ACCOUNT, viewModel, onBack)
            assertEquals(ProfileSubscreen.SETTINGS, viewModel.uiState.value.activeSubscreen)
            assertFalse(onBackCalled)

            handleSubscreenBack(ProfileSubscreen.PROFILE, viewModel, onBack)
            assertTrue(onBackCalled)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testProfileViewModelDeleteProfileSoftDeletesAndSignsOut() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val profileRepo = InMemoryProfileRepository(
                initialProfiles = emptyMap(),
                initialReviews = emptyMap()
            )
            val authRepo = InMemoryAuthRepository(
                profileRepository = profileRepo,
                testVerificationCode = "123456"
            )
            val registered = authRepo.register(
                email = StudentEmail("student@example.com"),
                password = "validPassword123",
                fullName = "Dominic Alfonso"
            )
            assertTrue(registered is Result.Success)
            val verifyResult = VerifyStudentEmail(authRepo)(registered.data.email, "123456")
            assertTrue(verifyResult is Result.Success)
            val profile = assertIs<EmailVerificationOutcome.ProfileAvailable>(verifyResult.data).profile

            val fixedTimestamp = "2026-10-06T19:00:00Z"
            val softDeleteProfile = SoftDeleteProfile(
                profileRepository = profileRepo,
                signOutUser = SignOutUser(authRepo),
                nowTimestamp = { fixedTimestamp }
            )
            val viewModel = createProfileViewModel(
                authRepository = authRepo,
                profileRepository = profileRepo,
                softDeleteProfile = softDeleteProfile
            )
            advanceUntilIdle()

            assertEquals(AuthSessionState.AUTHENTICATED, viewModel.uiState.value.authSessionState)
            assertNotNull(viewModel.uiState.value.profile)

            var successCallbackCalled = false
            viewModel.deleteProfile(onSuccess = { successCallbackCalled = true })
            advanceUntilIdle()

            assertTrue(successCallbackCalled)
            assertEquals(AuthSessionState.UNAUTHENTICATED, viewModel.uiState.value.authSessionState)
            assertNull(viewModel.uiState.value.profile)
            assertNull((RestoreAuthenticatedProfile(authRepo)() as Result.Success).data)

            val persisted = profileRepo.getProfile(profile.id)
            assertTrue(persisted is Result.Success)
            assertEquals(fixedTimestamp, persisted.data?.deletedAt)
            assertTrue(persisted.data?.isDeleted == true)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testProfileViewModelDeletionKeepsAuthenticatedStateWhenSignOutFails() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
            val delegate = InMemoryAuthRepository(profileRepo, testVerificationCode = "123456")
            val registered = delegate.register(
                StudentEmail("student@example.com"),
                "validPassword123",
                "Student Name"
            )
            assertTrue(registered is Result.Success)
            VerifyStudentEmail(delegate)(registered.data.email, "123456")
            val authRepo = SessionFailureAuthRepository(
                delegate = delegate,
                signOutError = IllegalStateException("Unable to clear session")
            )
            val viewModel = createProfileViewModel(authRepo, profileRepo)
            advanceUntilIdle()

            viewModel.deleteProfile()
            advanceUntilIdle()

            assertEquals(AuthSessionState.AUTHENTICATED, viewModel.uiState.value.authSessionState)
            assertEquals("Unable to clear session", viewModel.uiState.value.errorMessage)
            assertNotNull((RestoreAuthenticatedProfile(delegate)() as Result.Success).data)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testProfileViewModelDeletionFailureDoesNotSignOut() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
            val authRepo = InMemoryAuthRepository(profileRepo, testVerificationCode = "123456")
            val registered = authRepo.register(
                StudentEmail("student@example.com"),
                "validPassword123",
                "Student Name"
            )
            assertTrue(registered is Result.Success)
            VerifyStudentEmail(authRepo)(registered.data.email, "123456")
            val failingProfileRepo = object : ProfileRepository by profileRepo {
                override suspend fun softDelete(userId: UserId, deletedAt: String): Result<Unit> {
                    return Result.Error(IllegalStateException("Profile storage unavailable"))
                }
            }
            val viewModel = createProfileViewModel(
                authRepository = authRepo,
                profileRepository = profileRepo,
                softDeleteProfile = SoftDeleteProfile(failingProfileRepo, SignOutUser(authRepo))
            )
            advanceUntilIdle()

            viewModel.deleteProfile()
            advanceUntilIdle()

            assertEquals(AuthSessionState.AUTHENTICATED, viewModel.uiState.value.authSessionState)
            assertEquals("Profile storage unavailable", viewModel.uiState.value.errorMessage)
            assertNotNull((RestoreAuthenticatedProfile(authRepo)() as Result.Success).data)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
