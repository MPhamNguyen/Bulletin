package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.AuthenticateUser
import com.jdrms.bulletin.domain.profile.application.ManageProfile
import com.jdrms.bulletin.domain.profile.application.RestoreAuthenticatedProfile
import com.jdrms.bulletin.domain.profile.application.SignOutUser
import com.jdrms.bulletin.domain.profile.application.SubmitStudentReview
import com.jdrms.bulletin.domain.profile.application.UpdateStudentProfile
import com.jdrms.bulletin.domain.profile.application.VerifyStudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import com.jdrms.bulletin.domain.profile.domain.service.ProfileValidationPolicy
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileViewModelTest {

    private val policy = ProfileValidationPolicy()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testProfileViewModelRestoresAndClearsAuthenticatedSession() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
            val authRepo = InMemoryAuthRepository(profileRepo)
            val registered = authRepo.register(
                email = StudentEmail("student@example.com"),
                password = "validPassword123",
                fullName = "Student Name"
            )
            assertTrue(registered is Result.Success)

            val viewModel = ProfileViewModel(
                authenticateUser = AuthenticateUser(authRepo, policy),
                restoreAuthenticatedProfile = RestoreAuthenticatedProfile(authRepo),
                signOutUser = SignOutUser(authRepo),
                verifyStudentEmail = VerifyStudentEmail(authRepo),
                manageProfile = ManageProfile(profileRepo),
                updateStudentProfile = UpdateStudentProfile(profileRepo),
                submitStudentReview = SubmitStudentReview(profileRepo, policy)
            )
            advanceUntilIdle()

            assertEquals(AuthSessionState.AUTHENTICATED, viewModel.uiState.value.authSessionState)
            assertEquals("Student Name", viewModel.uiState.value.profile?.fullName)

            var signedOut = false
            viewModel.signOut { signedOut = true }
            advanceUntilIdle()

            assertTrue(signedOut)
            assertEquals(AuthSessionState.UNAUTHENTICATED, viewModel.uiState.value.authSessionState)
            assertNull(viewModel.uiState.value.profile)
            assertFalse(viewModel.uiState.value.isLoading)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testProfileViewModelReportsSessionRestoreFailure() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
            val authRepo = SessionFailureAuthRepository(
                delegate = InMemoryAuthRepository(profileRepo),
                restoreError = IllegalStateException("Session storage unavailable")
            )
            val viewModel = createProfileViewModel(authRepo, profileRepo)

            advanceUntilIdle()

            assertEquals(AuthSessionState.UNAUTHENTICATED, viewModel.uiState.value.authSessionState)
            assertEquals("Session storage unavailable", viewModel.uiState.value.errorMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testProfileViewModelKeepsSessionWhenSignOutFails() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
            val delegate = InMemoryAuthRepository(profileRepo)
            delegate.register(StudentEmail("student@example.com"), "validPassword123", "Student Name")
            val authRepo = SessionFailureAuthRepository(
                delegate = delegate,
                signOutError = IllegalStateException("Unable to clear session")
            )
            val viewModel = createProfileViewModel(authRepo, profileRepo)
            advanceUntilIdle()

            var signedOut = false
            viewModel.signOut { signedOut = true }
            advanceUntilIdle()

            assertFalse(signedOut)
            assertEquals(AuthSessionState.AUTHENTICATED, viewModel.uiState.value.authSessionState)
            assertEquals("Unable to clear session", viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isLoading)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createProfileViewModel(
        authRepository: AuthRepository,
        profileRepository: InMemoryProfileRepository
    ): ProfileViewModel {
        return ProfileViewModel(
            authenticateUser = AuthenticateUser(authRepository, policy),
            restoreAuthenticatedProfile = RestoreAuthenticatedProfile(authRepository),
            signOutUser = SignOutUser(authRepository),
            verifyStudentEmail = VerifyStudentEmail(authRepository),
            manageProfile = ManageProfile(profileRepository),
            updateStudentProfile = UpdateStudentProfile(profileRepository),
            submitStudentReview = SubmitStudentReview(profileRepository, policy)
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testProfileViewModelCreateAccountAndValidation() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
            val authRepo = InMemoryAuthRepository(profileRepo)
            val authenticateUser = AuthenticateUser(authRepo, policy)
            val verifyStudentEmail = VerifyStudentEmail(authRepo)
            val manageProfile = ManageProfile(profileRepo)
            val updateStudentProfile = UpdateStudentProfile(profileRepo)
            val submitStudentReview = SubmitStudentReview(profileRepo, policy)

            val viewModel = ProfileViewModel(
                authenticateUser = authenticateUser,
                restoreAuthenticatedProfile = RestoreAuthenticatedProfile(authRepo),
                signOutUser = SignOutUser(authRepo),
                verifyStudentEmail = verifyStudentEmail,
                manageProfile = manageProfile,
                updateStudentProfile = updateStudentProfile,
                submitStudentReview = submitStudentReview
            )
            advanceUntilIdle()

            // Blank first name check
            viewModel.createAccount("", "Doe", "test@example.com", "password123")
            advanceUntilIdle()
            assertEquals("First name is required.", viewModel.uiState.value.errorMessage)

            // Blank last name check
            viewModel.createAccount("John", "", "test@example.com", "password123")
            advanceUntilIdle()
            assertEquals("Last name is required.", viewModel.uiState.value.errorMessage)

            // Blank email check
            viewModel.createAccount("John", "Doe", "", "password123")
            advanceUntilIdle()
            assertEquals("Email is required.", viewModel.uiState.value.errorMessage)

            // Invalid email check
            viewModel.createAccount("John", "Doe", "notanemail", "password123")
            advanceUntilIdle()
            assertEquals("Invalid email address format.", viewModel.uiState.value.errorMessage)

            // Empty password check
            viewModel.createAccount("John", "Doe", "test@example.com", "")
            advanceUntilIdle()
            assertEquals("Password is required.", viewModel.uiState.value.errorMessage)

            // Short password check
            viewModel.createAccount("John", "Doe", "test@example.com", "123")
            advanceUntilIdle()
            assertEquals("Password must be at least 8 characters.", viewModel.uiState.value.errorMessage)

            // Successful account creation
            viewModel.createAccount("John", "Doe", "john.doe@example.com", "password123")
            runCurrent()

            val state = viewModel.uiState.value
            assertTrue(state.isAccountCreated)
            assertEquals("Account created successfully!", state.successMessage)
            assertNull(state.errorMessage)
            assertNotNull(state.profile)
            assertEquals("John Doe", state.profile.fullName)
            assertEquals("John Doe", state.profileDraft.fullName)

            advanceTimeBy(ProfileViewModel.FLASH_NOTIFICATION_DURATION_MILLIS)
            runCurrent()
            assertNull(viewModel.uiState.value.errorMessage)
            assertNull(viewModel.uiState.value.successMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testProfileViewModelUpdatesAndResetsProfileDraft() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
            val authRepo = InMemoryAuthRepository(profileRepo)
            val viewModel = ProfileViewModel(
                authenticateUser = AuthenticateUser(authRepo, policy),
                restoreAuthenticatedProfile = RestoreAuthenticatedProfile(authRepo),
                signOutUser = SignOutUser(authRepo),
                verifyStudentEmail = VerifyStudentEmail(authRepo),
                manageProfile = ManageProfile(profileRepo),
                updateStudentProfile = UpdateStudentProfile(profileRepo),
                submitStudentReview = SubmitStudentReview(profileRepo, policy)
            )
            advanceUntilIdle()
            viewModel.createAccount("John", "Doe", "john.doe@example.com", "password123")
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isProfileModified)

            viewModel.onProfileDraftChanged(
                viewModel.uiState.value.profileDraft.copy(
                    fullName = "Jane Doe",
                    major = "Computer Science",
                    university = "California State University - Long Beach",
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
            val authRepo = InMemoryAuthRepository(profileRepo)
            val authenticateUser = AuthenticateUser(authRepo, policy)
            val verifyStudentEmail = VerifyStudentEmail(authRepo)
            val manageProfile = ManageProfile(profileRepo)
            val updateStudentProfile = UpdateStudentProfile(profileRepo)
            val submitStudentReview = SubmitStudentReview(profileRepo, policy)

            val viewModel = ProfileViewModel(
                authenticateUser = authenticateUser,
                restoreAuthenticatedProfile = RestoreAuthenticatedProfile(authRepo),
                signOutUser = SignOutUser(authRepo),
                verifyStudentEmail = verifyStudentEmail,
                manageProfile = manageProfile,
                updateStudentProfile = updateStudentProfile,
                submitStudentReview = submitStudentReview
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
        } finally {
            Dispatchers.resetMain()
        }
    }
}

private class SessionFailureAuthRepository(
    private val delegate: AuthRepository,
    private val restoreError: Throwable? = null,
    private val signOutError: Throwable? = null
) : AuthRepository by delegate {

    override suspend fun getCurrentUser(): Result<StudentProfile?> {
        return restoreError?.let { Result.Error(it) } ?: delegate.getCurrentUser()
    }

    override suspend fun signOut(): Result<Unit> {
        return signOutError?.let { Result.Error(it) } ?: delegate.signOut()
    }
}
