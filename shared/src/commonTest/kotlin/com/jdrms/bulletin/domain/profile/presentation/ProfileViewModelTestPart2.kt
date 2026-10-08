package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.core.common.RefreshSignal
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
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import com.jdrms.bulletin.domain.profile.domain.repository.ProfilePhotoRepository
import com.jdrms.bulletin.domain.profile.domain.service.ProfileValidationPolicy
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

abstract class ProfileViewModelTestPart2 {

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
}
