package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.AuthenticateUser
import com.jdrms.bulletin.domain.profile.application.ManageProfile
import com.jdrms.bulletin.domain.profile.application.ProfileActiveListingsProvider
import com.jdrms.bulletin.domain.profile.application.ResendVerificationCode
import com.jdrms.bulletin.domain.profile.application.RestoreAuthenticatedProfile
import com.jdrms.bulletin.domain.profile.application.SignOutUser
import com.jdrms.bulletin.domain.profile.application.SoftDeleteProfile
import com.jdrms.bulletin.domain.profile.application.SubmitStudentReview
import com.jdrms.bulletin.domain.profile.application.UpdateStudentProfile
import com.jdrms.bulletin.domain.profile.application.UploadProfilePhoto
import com.jdrms.bulletin.domain.profile.application.VerifyStudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import com.jdrms.bulletin.domain.profile.domain.repository.ProfilePhotoRepository
import com.jdrms.bulletin.domain.profile.domain.service.ProfileValidationPolicy
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfilePhotoRepository
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

abstract class ProfileViewModelTestPart1 {

    protected val policy = ProfileValidationPolicy()

    protected fun createProfileViewModel(
        authRepository: AuthRepository,
        profileRepository: InMemoryProfileRepository,
        activeListingsProvider: ProfileActiveListingsProvider? = null,
        listingChangedSignal: RefreshSignal? = null,
        profilePhotoRepository: ProfilePhotoRepository? = null,
        softDeleteProfile: SoftDeleteProfile = SoftDeleteProfile(profileRepository, SignOutUser(authRepository))
    ): ProfileViewModel {
        return ProfileViewModel(
            ProfileViewModelDependencies(
                authenticateUser = AuthenticateUser(authRepository, policy),
                restoreAuthenticatedProfile = RestoreAuthenticatedProfile(authRepository),
                signOutUser = SignOutUser(authRepository),
                verifyStudentEmail = VerifyStudentEmail(authRepository),
                resendVerificationCode = ResendVerificationCode(authRepository),
                manageProfile = ManageProfile(profileRepository),
                updateStudentProfile = UpdateStudentProfile(profileRepository),
                submitStudentReview = SubmitStudentReview(profileRepository, policy),
                uploadProfilePhoto = profilePhotoRepository?.let {
                    UploadProfilePhoto(it, profileRepository)
                },
                activeListingsProvider = activeListingsProvider,
                listingChangedSignal = listingChangedSignal,
                softDeleteProfile = softDeleteProfile
            )
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun profilePhotoSelectionUploadsAndUpdatesVisibleProfileState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
            val authRepo = InMemoryAuthRepository(profileRepo, testVerificationCode = "123456")
            val registration = authRepo.register(
                StudentEmail("photo@school.edu"),
                "validPassword123",
                "Photo Student"
            ) as Result.Success
            VerifyStudentEmail(authRepo)(registration.data.email, "123456")
            val viewModel = createProfileViewModel(
                authRepository = authRepo,
                profileRepository = profileRepo,
                profilePhotoRepository = InMemoryProfilePhotoRepository()
            )
            advanceUntilIdle()

            viewModel.uploadProfilePhoto(
                byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 0x01),
                "image/jpeg"
            )
            assertTrue(viewModel.uiState.value.isPhotoUploading)
            runCurrent()

            assertFalse(viewModel.uiState.value.isPhotoUploading)
            assertTrue(viewModel.uiState.value.profile?.avatarUrl?.startsWith("memory://profile-photo/") == true)
            assertEquals("Profile photo updated", viewModel.uiState.value.successMessage)
            advanceUntilIdle()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun profilePhotoPickerErrorsAreExposedToTheScreen() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val profileRepo = InMemoryProfileRepository()
            val authRepo = InMemoryAuthRepository(profileRepo)
            val viewModel = createProfileViewModel(authRepo, profileRepo)

            viewModel.onProfilePhotoSelectionError("Unable to read the selected image.")

            assertEquals("Unable to read the selected image.", viewModel.uiState.value.errorMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testProfileViewModelRestoresAndClearsAuthenticatedSession() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
            val authRepo = InMemoryAuthRepository(profileRepo, testVerificationCode = "123456")
            val registered = authRepo.register(
                email = StudentEmail("student@example.com"),
                password = "validPassword123",
                fullName = "Student Name"
            )
            assertTrue(registered is Result.Success)
            VerifyStudentEmail(authRepo)(registered.data.email, "123456")

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
    fun testARecreatedProfileViewModelRestoresThePersistedSessionAndProfileDraft() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
            val authRepo = InMemoryAuthRepository(profileRepo, testVerificationCode = "123456")
            val registered = authRepo.register(
                email = StudentEmail("restore@example.com"),
                password = "validPassword123",
                fullName = "Restored Student"
            )
            assertTrue(registered is Result.Success)
            VerifyStudentEmail(authRepo)(registered.data.email, "123456")

            val firstViewModel = createProfileViewModel(authRepo, profileRepo)
            advanceUntilIdle()
            assertEquals(AuthSessionState.AUTHENTICATED, firstViewModel.uiState.value.authSessionState)

            val recreatedViewModel = createProfileViewModel(authRepo, profileRepo)
            advanceUntilIdle()

            assertEquals(AuthSessionState.AUTHENTICATED, recreatedViewModel.uiState.value.authSessionState)
            assertEquals("Restored Student", recreatedViewModel.uiState.value.profile?.fullName)
            assertEquals("Restored Student", recreatedViewModel.uiState.value.profileDraft.fullName)
            assertFalse(recreatedViewModel.uiState.value.isProfileModified)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testProfileViewModelSilentlyFallsBackWhenSessionRestoreFails() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
            val authRepo = SessionFailureAuthRepository(
                delegate = InMemoryAuthRepository(profileRepo, testVerificationCode = "123456"),
                restoreError = IllegalStateException("Session storage unavailable")
            )
            val viewModel = createProfileViewModel(authRepo, profileRepo)

            advanceUntilIdle()

            assertEquals(AuthSessionState.UNAUTHENTICATED, viewModel.uiState.value.authSessionState)
            assertNull(viewModel.uiState.value.errorMessage)
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
            val delegate = InMemoryAuthRepository(profileRepo, testVerificationCode = "123456")
            delegate.register(StudentEmail("student@example.com"), "validPassword123", "Student Name")
            VerifyStudentEmail(delegate)(StudentEmail("student@example.com"), "123456")
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

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testProfileViewModelCreateAccountAndValidation() = runTest {
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

            val viewModel = createProfileViewModel(authRepo, profileRepo)
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

            // Non-university email check
            viewModel.createAccount("John", "Doe", "john.doe@example.com", "password123")
            advanceUntilIdle()
            assertEquals(
                "Bulletin requires a valid .edu university email.",
                viewModel.uiState.value.errorMessage
            )

            // Empty password check
            viewModel.createAccount("John", "Doe", "test@example.com", "")
            advanceUntilIdle()
            assertEquals("Password is required.", viewModel.uiState.value.errorMessage)

            // Short password check
            viewModel.createAccount("John", "Doe", "test@example.com", "123")
            advanceUntilIdle()
            assertEquals("Password must be at least 8 characters.", viewModel.uiState.value.errorMessage)

            // Successful account creation
            viewModel.createAccount("John", "Doe", "john.doe@school.edu", "password123")
            runCurrent()

            viewModel.verifyEmail("john.doe@school.edu", "123456")
            runCurrent()
            val state = viewModel.uiState.value
            assertTrue(state.isAccountCreated)
            assertEquals(AuthSessionState.AUTHENTICATED, state.authSessionState)
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
}
internal class SessionFailureAuthRepository(
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
