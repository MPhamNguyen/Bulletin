package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile

enum class ProfilePhotoAction { UPLOADING, REMOVING }

data class EditProfileUiState(
    val profile: StudentProfile? = null,
    val profileDraft: ProfileDraft = ProfileDraft(),
    val profileFormErrors: ProfileFormErrors = ProfileFormErrors(),
    val isLoading: Boolean = false,
    val photoAction: ProfilePhotoAction? = null,
    val errorMessage: String? = null
) {
    val isProfileModified: Boolean get() = profile != null && ProfileDraft.from(profile) != profileDraft
    val isPhotoBusy: Boolean get() = photoAction != null
}
