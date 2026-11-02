package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.common.UserMessenger
import com.jdrms.bulletin.domain.profile.application.SessionRepository
import com.jdrms.bulletin.domain.profile.application.SessionState
import com.jdrms.bulletin.domain.profile.application.UpdateStudentProfile
import com.jdrms.bulletin.domain.profile.application.UploadProfilePhoto
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class EditProfilePhotoInteraction(
    private val state: MutableStateFlow<EditProfileUiState>,
    private val scope: CoroutineScope,
    private val session: SessionRepository,
    private val updateStudentProfile: UpdateStudentProfile,
    private val uploadProfilePhoto: UploadProfilePhoto,
    private val messenger: UserMessenger
) {
    fun upload(bytes: ByteArray, mediaType: String) = changePhoto(ProfilePhotoAction.UPLOADING) { profile ->
        uploadProfilePhoto(profile, bytes, mediaType)
    }

    fun remove() {
        if (state.value.profile?.avatarUrl.isNullOrBlank()) return
        changePhoto(ProfilePhotoAction.REMOVING) { profile ->
            updateStudentProfile(
                profile,
                profile.fullName,
                profile.major,
                profile.university,
                profile.bio,
                avatarUrl = null
            )
        }
    }

    private fun changePhoto(
        action: ProfilePhotoAction,
        operation: suspend (StudentProfile) -> Result<StudentProfile>
    ) {
        if (state.value.isPhotoBusy || state.value.isLoading) return
        val profile = state.value.profile ?: run {
            state.update { it.copy(errorMessage = "Profile is unavailable.") }
            return
        }
        state.update { it.copy(photoAction = action, errorMessage = null) }
        scope.launch {
            val result = operation(profile)
            if ((session.state.value as? SessionState.Authenticated)?.profile?.id != profile.id) return@launch
            when (result) {
                is Result.Success -> {
                    session.authenticated(result.data)
                    state.update { it.copy(photoAction = null) }
                    messenger.show(
                        if (action == ProfilePhotoAction.REMOVING) "Profile photo removed" else "Profile photo updated"
                    )
                }
                is Result.Error -> state.update {
                    it.copy(
                        photoAction = null,
                        errorMessage = result.exception.message ?: if (action == ProfilePhotoAction.REMOVING) {
                            "Failed to remove profile photo"
                        } else {
                            "Failed to upload profile photo"
                        }
                    )
                }
            }
        }
    }
}
