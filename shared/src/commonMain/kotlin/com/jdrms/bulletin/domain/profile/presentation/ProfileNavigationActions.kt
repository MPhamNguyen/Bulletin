package com.jdrms.bulletin.domain.profile.presentation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class ProfileNavigationActions internal constructor(
    private val uiState: MutableStateFlow<ProfileUiState>,
    private val cancelFlashNotification: () -> Unit
) {
    fun openSettings() = navigateTo(ProfileSubscreen.SETTINGS)

    fun openProfile() = navigateTo(ProfileSubscreen.PROFILE)

    fun openBookmarkedListings() = navigateTo(ProfileSubscreen.BOOKMARKED_LISTINGS)

    fun openNotifications() = navigateTo(ProfileSubscreen.NOTIFICATIONS)

    fun openPrivacy() = navigateTo(ProfileSubscreen.PRIVACY)

    fun openHelpAndSupport() = navigateTo(ProfileSubscreen.HELP_AND_SUPPORT)

    fun openTermsAndConditions() = navigateTo(ProfileSubscreen.TERMS_AND_CONDITIONS)

    fun openPublicProfile() = navigateTo(ProfileSubscreen.PUBLIC_PROFILE)

    fun startEditingProfile() {
        val profile = uiState.value.profile
        uiState.update {
            it.copy(
                profileDraft = profile?.let(ProfileDraft::from) ?: ProfileDraft(),
                profileFormErrors = ProfileFormErrors(),
                isEditingProfile = true,
                activeSubscreen = ProfileSubscreen.EDIT_ACCOUNT,
                editReturnSubscreen = it.activeSubscreen,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun cancelEditingProfile() {
        val profile = uiState.value.profile
        cancelFlashNotification()
        uiState.update {
            it.copy(
                profileDraft = profile?.let(ProfileDraft::from) ?: ProfileDraft(),
                profileFormErrors = ProfileFormErrors(),
                isEditingProfile = false,
                activeSubscreen = it.editReturnSubscreen,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    private fun navigateTo(subscreen: ProfileSubscreen) {
        cancelFlashNotification()
        uiState.update {
            it.copy(
                activeSubscreen = subscreen,
                isEditingProfile = false,
                errorMessage = null
            )
        }
    }
}
