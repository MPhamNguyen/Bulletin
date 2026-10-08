package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.domain.profile.application.AuthenticateUser
import com.jdrms.bulletin.domain.profile.application.ManageProfile
import com.jdrms.bulletin.domain.profile.application.ResendVerificationCode
import com.jdrms.bulletin.domain.profile.application.RestoreAuthenticatedProfile
import com.jdrms.bulletin.domain.profile.application.SignOutUser
import com.jdrms.bulletin.domain.profile.application.SoftDeleteProfile
import com.jdrms.bulletin.domain.profile.application.SubmitStudentReview
import com.jdrms.bulletin.domain.profile.application.UpdateStudentProfile
import com.jdrms.bulletin.domain.profile.application.VerifyStudentEmail
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileViewModelTestPart3 : ProfileViewModelTestPart1() {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testProfileViewModelUpdatesAndResetsProfileDraft() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
            val authRepo = InMemoryAuthRepository(profileRepo, testVerificationCode = "123456")
            val viewModel = ProfileViewModel(
                ProfileViewModelDependencies(
                    authenticateUser = AuthenticateUser(authRepo, policy),
                    restoreAuthenticatedProfile = RestoreAuthenticatedProfile(authRepo),
                    signOutUser = SignOutUser(authRepo),
                    verifyStudentEmail = VerifyStudentEmail(authRepo),
                    resendVerificationCode = ResendVerificationCode(authRepo),
                    manageProfile = ManageProfile(profileRepo),
                    updateStudentProfile = UpdateStudentProfile(profileRepo),
                    submitStudentReview = SubmitStudentReview(profileRepo, policy),
                    softDeleteProfile = SoftDeleteProfile(profileRepo, SignOutUser(authRepo))
                )
            )
            advanceUntilIdle()
            viewModel.createAccount("John", "Doe", "john.doe@school.edu", "password123")
            advanceUntilIdle()
            viewModel.verifyEmail("john.doe@school.edu", "123456")
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isProfileModified)

            viewModel.onProfileDraftChanged(
                viewModel.uiState.value.profileDraft.copy(
                    fullName = "Jane Doe",
                    major = "Computer Science",
                    university = "California State University, Long Beach (CSULB)",
                    bio = "Campus student"
                )
            )
            assertTrue(viewModel.uiState.value.isProfileModified)

            viewModel.updateProfileDetails()
            runCurrent()

            val updatedState = viewModel.uiState.value
            assertFalse(updatedState.isProfileModified)
            assertEquals("Jane Doe", updatedState.profile?.fullName)
            assertEquals("Computer Science", updatedState.profile?.major)
            assertEquals("California State University, Long Beach", updatedState.profile?.university)
            assertEquals("Profile updated", updatedState.successMessage)

            advanceTimeBy(ProfileViewModel.FLASH_NOTIFICATION_DURATION_MILLIS - 1)
            runCurrent()
            assertEquals("Profile updated", viewModel.uiState.value.successMessage)
            advanceTimeBy(1)
            runCurrent()
            assertNull(viewModel.uiState.value.successMessage)

            viewModel.onProfileDraftChanged(updatedState.profileDraft.copy(fullName = "Unsaved Name"))
            assertTrue(viewModel.uiState.value.isProfileModified)

            viewModel.resetProfileDraft()
            assertFalse(viewModel.uiState.value.isProfileModified)
            assertEquals("Jane Doe", viewModel.uiState.value.profileDraft.fullName)

            viewModel.onProfileDraftChanged(viewModel.uiState.value.profileDraft.copy(fullName = " "))
            assertTrue(viewModel.uiState.value.isProfileModified)
            viewModel.updateProfileDetails()
            advanceUntilIdle()
            assertEquals("Full name is required.", viewModel.uiState.value.errorMessage)
            assertEquals("Jane Doe", viewModel.uiState.value.profile?.fullName)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testProfileViewModelLoginValidation() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
            val authRepo = InMemoryAuthRepository(profileRepo, testVerificationCode = "123456")
            val authenticateUser = AuthenticateUser(authRepo, policy)
            val verifyStudentEmail = VerifyStudentEmail(authRepo)
            val manageProfile = ManageProfile(profileRepo)
            val updateStudentProfile = UpdateStudentProfile(profileRepo)
            val submitStudentReview = SubmitStudentReview(profileRepo, policy)

            val viewModel = ProfileViewModel(
                ProfileViewModelDependencies(
                    authenticateUser = authenticateUser,
                    restoreAuthenticatedProfile = RestoreAuthenticatedProfile(authRepo),
                    signOutUser = SignOutUser(authRepo),
                    verifyStudentEmail = verifyStudentEmail,
                    resendVerificationCode = ResendVerificationCode(authRepo),
                    manageProfile = manageProfile,
                    updateStudentProfile = updateStudentProfile,
                    submitStudentReview = submitStudentReview,
                    softDeleteProfile = SoftDeleteProfile(profileRepo, SignOutUser(authRepo))
                )
            )
            advanceUntilIdle()

            // Empty email check
            viewModel.login("", "password123")
            advanceUntilIdle()
            assertEquals("Email is required.", viewModel.uiState.value.errorMessage)

            // Invalid email check
            viewModel.login("invalid-email", "password123")
            advanceUntilIdle()
            assertEquals("Invalid email address format.", viewModel.uiState.value.errorMessage)

            // Empty password check
            viewModel.login("john@example.com", "")
            advanceUntilIdle()
            assertEquals("Password is required.", viewModel.uiState.value.errorMessage)

            // Account not found check
            viewModel.login("notfound@csulb.edu", "password123")
            advanceUntilIdle()
            assertEquals(
                "Account not found. Please check your email or create an account.",
                viewModel.uiState.value.errorMessage
            )

            // Register an account and test wrong password vs correct password
            viewModel.createAccount("Dominic", "Alfonso", "dominic@csulb.edu", "correctPassword123")
            advanceUntilIdle()
            viewModel.verifyEmail("dominic@csulb.edu", "123456")
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isAccountCreated)

            // Incorrect password check
            viewModel.login("dominic@csulb.edu", "wrongPassword123")
            advanceUntilIdle()
            assertEquals("Incorrect password. Please try again.", viewModel.uiState.value.errorMessage)

            // Successful login check with onSuccess callback
            var loginSuccessCalled = false
            viewModel.login("dominic@csulb.edu", "correctPassword123") {
                loginSuccessCalled = true
            }
            advanceUntilIdle()
            assertTrue(loginSuccessCalled)
            assertNull(viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isAccountCreated)
            assertEquals(AuthSessionState.AUTHENTICATED, viewModel.uiState.value.authSessionState)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testProfileViewModelEditingFlow() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
            val authRepo = InMemoryAuthRepository(profileRepo, testVerificationCode = "123456")
            val viewModel = createProfileViewModel(authRepo, profileRepo)
            advanceUntilIdle()

            viewModel.createAccount("Alex", "Student", "alex@csulb.edu", "password123")
            advanceUntilIdle()
            viewModel.verifyEmail("alex@csulb.edu", "123456")
            advanceUntilIdle()

            // Initial state: Landing profile (isEditingProfile == false)
            assertFalse(viewModel.uiState.value.isEditingProfile)

            // Start editing profile
            viewModel.startEditingProfile()
            assertTrue(viewModel.uiState.value.isEditingProfile)
            assertEquals("Alex Student", viewModel.uiState.value.profileDraft.fullName)

            // Change draft and cancel
            viewModel.onProfileDraftChanged(
                viewModel.uiState.value.profileDraft.copy(fullName = "Unsaved Name")
            )
            assertTrue(viewModel.uiState.value.isProfileModified)
            viewModel.cancelEditingProfile()
            assertFalse(viewModel.uiState.value.isEditingProfile)
            assertFalse(viewModel.uiState.value.isProfileModified)
            assertEquals("Alex Student", viewModel.uiState.value.profileDraft.fullName)
            assertEquals(ProfileSubscreen.PROFILE, viewModel.uiState.value.activeSubscreen)

            // Editing from Settings returns to Settings when cancelled.
            viewModel.openSettings()
            viewModel.openEditAccount()
            viewModel.cancelEditingProfile()
            assertEquals(ProfileSubscreen.SETTINGS, viewModel.uiState.value.activeSubscreen)
            viewModel.openProfile()

            // Start editing again, trigger invalid update
            viewModel.startEditingProfile()
            assertTrue(viewModel.uiState.value.isEditingProfile)
            viewModel.onProfileDraftChanged(viewModel.uiState.value.profileDraft.copy(fullName = "   "))
            viewModel.updateProfileDetails()
            advanceUntilIdle()
            // Editing state preserved on validation failure
            assertTrue(viewModel.uiState.value.isEditingProfile)
            assertEquals("Full name is required.", viewModel.uiState.value.errorMessage)

            // Valid update: updates profile, returns to landing (isEditingProfile = false)
            viewModel.onProfileDraftChanged(
                viewModel.uiState.value.profileDraft.copy(
                    fullName = "Alex Updated",
                    major = "Computer Science"
                )
            )
            viewModel.updateProfileDetails()
            runCurrent()

            assertFalse(viewModel.uiState.value.isEditingProfile)
            assertEquals("Alex Updated", viewModel.uiState.value.profile?.fullName)
            assertEquals("Computer Science", viewModel.uiState.value.profile?.major)
            assertEquals("Profile updated", viewModel.uiState.value.successMessage)
            advanceUntilIdle()
        } finally {
            Dispatchers.resetMain()
        }
    }
}
