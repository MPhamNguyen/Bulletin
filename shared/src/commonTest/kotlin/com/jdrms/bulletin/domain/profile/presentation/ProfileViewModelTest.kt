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
import com.jdrms.bulletin.domain.profile.domain.model.EmailVerificationOutcome
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import com.jdrms.bulletin.domain.profile.domain.repository.ProfilePhotoRepository
import com.jdrms.bulletin.domain.profile.domain.repository.ProfileRepository
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
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Suppress("LargeClass")
class ProfileViewModelTest {

    private val policy = ProfileValidationPolicy()

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

    private fun createProfileViewModel(
        authRepository: AuthRepository,
        profileRepository: InMemoryProfileRepository,
        activeListingsProvider: ProfileActiveListingsProvider? = null,
        listingChangedSignal: RefreshSignal? = null,
        profilePhotoRepository: ProfilePhotoRepository? = null,
        softDeleteProfile: SoftDeleteProfile = SoftDeleteProfile(profileRepository, SignOutUser(authRepository))
    ): ProfileViewModel {
        return ProfileViewModel(
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

    @OptIn(ExperimentalCoroutinesApi::class)
    @Suppress("LongMethod")
    @Test
    fun testProfileViewModelUpdatesAndResetsProfileDraft() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
            val authRepo = InMemoryAuthRepository(profileRepo, testVerificationCode = "123456")
            val viewModel = ProfileViewModel(
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

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testProfileViewModelSubscreenNavigation() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
            val authRepo = InMemoryAuthRepository(profileRepo, testVerificationCode = "123456")
            val viewModel = createProfileViewModel(authRepo, profileRepo)
            advanceUntilIdle()

            viewModel.createAccount("Taylor", "Swift", "taylor@csulb.edu", "password123")
            advanceUntilIdle()
            viewModel.verifyEmail("taylor@csulb.edu", "123456")
            advanceUntilIdle()

            assertEquals(ProfileSubscreen.PROFILE, viewModel.uiState.value.activeSubscreen)

            viewModel.openSettings()
            assertEquals(ProfileSubscreen.SETTINGS, viewModel.uiState.value.activeSubscreen)

            viewModel.openEditAccount()
            assertEquals(ProfileSubscreen.EDIT_ACCOUNT, viewModel.uiState.value.activeSubscreen)
            viewModel.closeEditAccount()
            assertEquals(ProfileSubscreen.SETTINGS, viewModel.uiState.value.activeSubscreen)

            viewModel.closeSettings()
            assertEquals(ProfileSubscreen.PROFILE, viewModel.uiState.value.activeSubscreen)

            viewModel.openBookmarkedListings()
            assertEquals(ProfileSubscreen.BOOKMARKED_LISTINGS, viewModel.uiState.value.activeSubscreen)
            viewModel.closeBookmarkedListings()
            assertEquals(ProfileSubscreen.PROFILE, viewModel.uiState.value.activeSubscreen)

            viewModel.openNotifications()
            assertEquals(ProfileSubscreen.NOTIFICATIONS, viewModel.uiState.value.activeSubscreen)
            viewModel.closeNotifications()
            assertEquals(ProfileSubscreen.SETTINGS, viewModel.uiState.value.activeSubscreen)

            viewModel.openPrivacy()
            assertEquals(ProfileSubscreen.PRIVACY, viewModel.uiState.value.activeSubscreen)
            viewModel.closePrivacy()
            assertEquals(ProfileSubscreen.SETTINGS, viewModel.uiState.value.activeSubscreen)

            viewModel.openHelpAndSupport()
            assertEquals(ProfileSubscreen.HELP_AND_SUPPORT, viewModel.uiState.value.activeSubscreen)
            viewModel.closeHelpAndSupport()
            assertEquals(ProfileSubscreen.SETTINGS, viewModel.uiState.value.activeSubscreen)

            viewModel.openTermsAndConditions()
            assertEquals(ProfileSubscreen.TERMS_AND_CONDITIONS, viewModel.uiState.value.activeSubscreen)
            viewModel.closeTermsAndConditions()
            assertEquals(ProfileSubscreen.SETTINGS, viewModel.uiState.value.activeSubscreen)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testProfileViewModelProfileUpdate() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
            val authRepo = InMemoryAuthRepository(profileRepo, testVerificationCode = "123456")
            val viewModel = createProfileViewModel(authRepo, profileRepo)
            advanceUntilIdle()

            viewModel.createAccount("Taylor", "Swift", "taylor@csulb.edu", "password123")
            advanceUntilIdle()
            viewModel.verifyEmail("taylor@csulb.edu", "123456")
            advanceUntilIdle()

            viewModel.openEditAccount()
            viewModel.onProfileDraftChanged(
                viewModel.uiState.value.profileDraft
            )
            viewModel.updateProfileDetails()
            runCurrent()

            assertEquals(ProfileSubscreen.PROFILE, viewModel.uiState.value.activeSubscreen)
            assertFalse(viewModel.uiState.value.isEditingProfile)
            assertEquals("Profile updated", viewModel.uiState.value.successMessage)
            advanceUntilIdle()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testProfileViewModelPublicProfileNavigation() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
            val authRepo = InMemoryAuthRepository(profileRepo)
            val viewModel = createProfileViewModel(authRepo, profileRepo)
            advanceUntilIdle()

            viewModel.openPublicProfile()
            assertEquals(ProfileSubscreen.PUBLIC_PROFILE, viewModel.uiState.value.activeSubscreen)

            viewModel.closePublicProfile()
            assertEquals(ProfileSubscreen.SETTINGS, viewModel.uiState.value.activeSubscreen)

            assertEquals(0, viewModel.uiState.value.activeListingsCount)
            assertEquals(18, viewModel.uiState.value.itemsSoldCount)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testProfileViewModelLoadsActiveListingsCountFromProvider() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
            val authRepo = InMemoryAuthRepository(profileRepo, testVerificationCode = "123456")
            var countToReturn = 7
            val fakeProvider = ProfileActiveListingsProvider { countToReturn }
            val viewModel = createProfileViewModel(
                authRepository = authRepo,
                profileRepository = profileRepo,
                activeListingsProvider = fakeProvider
            )
            advanceUntilIdle()

            viewModel.createAccount("Provider", "User", "provider@csulb.edu", "password123")
            advanceUntilIdle()
            viewModel.verifyEmail("provider@csulb.edu", "123456")
            advanceUntilIdle()

            assertEquals(7, viewModel.uiState.value.activeListingsCount)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testProfileViewModelRefreshesActiveListingsCountOnSignal() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
            val authRepo = InMemoryAuthRepository(profileRepo, testVerificationCode = "123456")
            val signal = RefreshSignal()
            var currentCount = 3
            val fakeProvider = ProfileActiveListingsProvider { currentCount }

            val viewModel = createProfileViewModel(
                authRepository = authRepo,
                profileRepository = profileRepo,
                activeListingsProvider = fakeProvider,
                listingChangedSignal = signal
            )
            advanceUntilIdle()

            viewModel.createAccount("Refresh", "User", "refresh@csulb.edu", "password123")
            advanceUntilIdle()
            viewModel.verifyEmail("refresh@csulb.edu", "123456")
            advanceUntilIdle()
            assertEquals(3, viewModel.uiState.value.activeListingsCount)

            currentCount = 5
            signal.emit()
            advanceUntilIdle()

            assertEquals(5, viewModel.uiState.value.activeListingsCount)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testProfileViewModelRefreshActiveListingsDirectInvocation() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
            val authRepo = InMemoryAuthRepository(profileRepo, testVerificationCode = "123456")
            var currentCount = 2
            val fakeProvider = ProfileActiveListingsProvider { currentCount }

            val viewModel = createProfileViewModel(
                authRepository = authRepo,
                profileRepository = profileRepo,
                activeListingsProvider = fakeProvider
            )
            advanceUntilIdle()

            viewModel.createAccount("Direct", "Refresh", "direct@csulb.edu", "password123")
            advanceUntilIdle()
            viewModel.verifyEmail("direct@csulb.edu", "123456")
            advanceUntilIdle()
            assertEquals(2, viewModel.uiState.value.activeListingsCount)

            currentCount = 4
            viewModel.refreshActiveListings()
            advanceUntilIdle()

            assertEquals(4, viewModel.uiState.value.activeListingsCount)
        } finally {
            Dispatchers.resetMain()
        }
    }

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
