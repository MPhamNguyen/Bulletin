package com.jdrms.bulletin.domain.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Holds profile state and composes the focused collaborators used by profile destinations. */
class ProfileViewModel(
    dependencies: ProfileViewModelDependencies
) : ViewModel() {
    private val state = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = state.asStateFlow()
    val actions: ProfileViewModelActions

    init {
        val notifications = ProfileNotificationActions(state, viewModelScope)
        val navigation = ProfileNavigationActions(state, notifications::cancel)
        val overview = ProfileOverviewActions(
            state,
            viewModelScope,
            dependencies.manageProfile,
            dependencies.activeListingsProvider,
            dependencies.listingChangedSignal
        )
        val authentication = ProfileAuthenticationActions(
            state,
            viewModelScope,
            dependencies.authenticateUser,
            dependencies.restoreAuthenticatedProfile,
            dependencies.verifyStudentEmail,
            dependencies.resendVerificationCode,
            dependencies.manageProfile,
            dependencies.activeListingsProvider,
            dependencies.policy,
            notifications::show
        )
        val editing = ProfileEditingActions(
            state,
            viewModelScope,
            dependencies.updateStudentProfile,
            dependencies.uploadProfilePhoto,
            notifications::cancel,
            notifications::show
        )
        val account = ProfileAccountActions(
            state,
            viewModelScope,
            dependencies.signOutUser,
            dependencies.softDeleteProfile,
            dependencies.submitStudentReview,
            overview::loadProfile,
            notifications::cancel
        )
        actions = ProfileViewModelActions(
            navigation,
            ProfileWorkflowActions(authentication, editing, overview, account),
            notifications
        )
        authentication.restoreSession()
    }

    companion object {
        const val FLASH_NOTIFICATION_DURATION_MILLIS = 3_000L
        val DEFAULT_USER_ID = UserId("current_student")
    }
}
