package com.jdrms.bulletin.domain.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Coordinates profile presentation collaborators while keeping the existing screen-facing API stable. */
class ProfileViewModel(
    dependencies: ProfileViewModelDependencies
) : ViewModel() {
    private val defaultUserId = dependencies.defaultUserId
    private val state = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = state.asStateFlow()
    private var flashNotificationJob: Job? = null

    private val navigation = ProfileNavigationActions(state, ::cancelFlashNotification)
    private val editing = ProfileEditingActions(
        state,
        viewModelScope,
        dependencies.updateStudentProfile,
        dependencies.uploadProfilePhoto,
        ::cancelFlashNotification,
        ::showFlashNotification
    )
    private val overview = ProfileOverviewActions(
        state,
        viewModelScope,
        dependencies.manageProfile,
        dependencies.activeListingsProvider,
        dependencies.listingChangedSignal
    )
    private val authentication = ProfileAuthenticationActions(
        state,
        viewModelScope,
        dependencies.authenticateUser,
        dependencies.restoreAuthenticatedProfile,
        dependencies.verifyStudentEmail,
        dependencies.resendVerificationCode,
        dependencies.manageProfile,
        dependencies.activeListingsProvider,
        dependencies.policy,
        ::showFlashNotification
    )
    private val account = ProfileAccountActions(
        state,
        viewModelScope,
        dependencies.signOutUser,
        dependencies.softDeleteProfile,
        dependencies.submitStudentReview,
        overview::loadProfile,
        ::cancelFlashNotification
    )

    init {
        authentication.restoreSession()
    }

    fun refreshActiveListings() = overview.refreshActiveListings()
    fun loadProfile(userId: UserId = defaultUserId) = overview.loadProfile(userId)
    fun createAccount(
        firstName: String,
        lastName: String,
        emailStr: String,
        passwordStr: String,
        university: String = ""
    ) =
        authentication.createAccount(firstName, lastName, emailStr, passwordStr, university)
    fun retryVerifiedProfile() = authentication.retryVerifiedProfile()
    fun clearMessages() {
        cancelFlashNotification()
        state.value = state.value.copy(errorMessage = null, successMessage = null)
    }
    fun resetRegistration() = authentication.resetRegistration(::cancelFlashNotification)
    fun login(
        emailStr: String,
        pass: String,
        onSuccess: () -> Unit = {}
    ) = authentication.login(emailStr, pass, onSuccess)
    fun verifyEmail(emailStr: String, code: String) = authentication.verifyEmail(emailStr, code)
    fun resendEmailCode(emailStr: String) = authentication.resendEmailCode(emailStr)

    fun openSettings() = navigation.openSettings()
    fun openProfile() = navigation.openProfile()
    fun closeSettings() = openProfile()
    fun openBookmarkedListings() = navigation.openBookmarkedListings()
    fun closeBookmarkedListings() = openProfile()
    fun openNotifications() = navigation.openNotifications()
    fun closeNotifications() = openSettings()
    fun openPrivacy() = navigation.openPrivacy()
    fun closePrivacy() = openSettings()
    fun openHelpAndSupport() = navigation.openHelpAndSupport()
    fun closeHelpAndSupport() = openSettings()
    fun openTermsAndConditions() = navigation.openTermsAndConditions()
    fun closeTermsAndConditions() = openSettings()
    fun openPublicProfile() = navigation.openPublicProfile()
    fun closePublicProfile() = openSettings()
    fun openEditAccount() = startEditingProfile()
    fun closeEditAccount() = cancelEditingProfile()
    fun startEditingProfile() = navigation.startEditingProfile()
    fun cancelEditingProfile() = navigation.cancelEditingProfile()
    fun onProfileDraftChanged(draft: ProfileDraft) = editing.onDraftChanged(draft)
    fun resetProfileDraft() = editing.resetDraft()
    fun updateProfileDetails() = editing.updateDetails()
    fun uploadProfilePhoto(bytes: ByteArray, mediaType: String) = editing.uploadPhoto(bytes, mediaType)
    fun onProfilePhotoSelectionError(message: String) = editing.onPhotoSelectionError(message)

    fun signOut(onSuccess: () -> Unit = {}) = account.signOut(onSuccess)
    fun deleteProfile(onSuccess: () -> Unit = {}) = account.deleteProfile(onSuccess)
    fun showReviewModal(show: Boolean) = account.showReviewModal(show)
    fun onScoreChanged(score: Int) = account.onScoreChanged(score)
    fun onCommentChanged(comment: String) = account.onCommentChanged(comment)
    fun submitReview(reviewerId: String = "peer_reviewer", reviewerName: String = "Campus Peer") =
        account.submitReview(reviewerId, reviewerName)

    private fun cancelFlashNotification() {
        flashNotificationJob?.cancel()
        flashNotificationJob = null
    }

    private fun showFlashNotification(message: String) {
        cancelFlashNotification()
        state.value = state.value.copy(successMessage = message)
        flashNotificationJob = viewModelScope.launch {
            delay(FLASH_NOTIFICATION_DURATION_MILLIS)
            if (state.value.successMessage == message) state.value = state.value.copy(successMessage = null)
        }
    }

    companion object { const val FLASH_NOTIFICATION_DURATION_MILLIS = 3_000L }
}
