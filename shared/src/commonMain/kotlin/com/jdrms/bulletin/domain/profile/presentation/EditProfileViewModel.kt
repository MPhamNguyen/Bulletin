package com.jdrms.bulletin.domain.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.common.UserMessenger
import com.jdrms.bulletin.domain.profile.application.SessionRepository
import com.jdrms.bulletin.domain.profile.application.SessionState
import com.jdrms.bulletin.domain.profile.application.UpdateStudentProfile
import com.jdrms.bulletin.domain.profile.application.UploadProfilePhoto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditProfileViewModel(
    private val sessionRepository: SessionRepository,
    private val updateStudentProfile: UpdateStudentProfile,
    uploadProfilePhoto: UploadProfilePhoto,
    private val messenger: UserMessenger
) : ViewModel() {
    private val state = MutableStateFlow(EditProfileUiState())
    val uiState = state.asStateFlow()
    private val photos = EditProfilePhotoInteraction(
        state,
        viewModelScope,
        sessionRepository,
        updateStudentProfile,
        uploadProfilePhoto,
        messenger
    )

    init {
        viewModelScope.launch {
            sessionRepository.state.collect { session ->
                val profile = (session as? SessionState.Authenticated)?.profile
                state.update {
                    val previousProfile = it.profile
                    val editableDetailsUnchanged = previousProfile != null && profile != null &&
                        previousProfile.id == profile.id &&
                        previousProfile.fullName == profile.fullName &&
                        previousProfile.major == profile.major &&
                        previousProfile.university == profile.university &&
                        previousProfile.bio == profile.bio
                    it.copy(
                        profile = profile,
                        photoAction = it.photoAction.takeIf { profile != null && previousProfile?.id == profile.id },
                        profileDraft = if (editableDetailsUnchanged) {
                            it.profileDraft
                        } else {
                            profile?.let(ProfileDraft::from) ?: ProfileDraft()
                        }
                    )
                }
            }
        }
    }

    fun begin() {
        val profile = state.value.profile
        state.update {
            it.copy(
                profileDraft = profile?.let(ProfileDraft::from) ?: ProfileDraft(),
                profileFormErrors = ProfileFormErrors(),
                errorMessage = null
            )
        }
    }

    fun onDraftChanged(draft: ProfileDraft) {
        state.update { current ->
            current.copy(
                profileDraft = draft,
                profileFormErrors = current.profileFormErrors.copy(
                    school = current.profileFormErrors.school.takeUnless {
                        draft.university != current.profileDraft.university
                    },
                    major = current.profileFormErrors.major.takeUnless {
                        draft.major != current.profileDraft.major
                    }
                ),
                errorMessage = null
            )
        }
    }

    fun save(onSaved: () -> Unit) {
        if (state.value.isLoading || state.value.isPhotoBusy) return
        val profile = state.value.profile
        if (profile == null) {
            state.update { it.copy(errorMessage = "Profile is unavailable.") }
            return
        }
        val draft = state.value.profileDraft
        val errors = validateDraftForUpdate(draft, ProfileDraft.from(profile))
        if (!errors.isValid) {
            state.update { it.copy(profileFormErrors = errors) }
            return
        }
        state.update { it.copy(isLoading = true, profileFormErrors = ProfileFormErrors(), errorMessage = null) }
        viewModelScope.launch {
            when (
                val result = updateStudentProfile(
                    profile,
                    draft.fullName,
                    draft.major,
                    draft.university,
                    draft.bio
                )
            ) {
                is Result.Success -> {
                    sessionRepository.authenticated(result.data)
                    state.update {
                        it.copy(profile = result.data, profileDraft = ProfileDraft.from(result.data), isLoading = false)
                    }
                    messenger.show("Profile updated")
                    onSaved()
                }
                is Result.Error -> state.update {
                    it.copy(isLoading = false, errorMessage = result.exception.message ?: "Failed to update profile")
                }
            }
        }
    }

    fun uploadPhoto(bytes: ByteArray, mediaType: String) = photos.upload(bytes, mediaType)

    fun removePhoto() = photos.remove()

    fun onPhotoSelectionError(message: String) = state.update { it.copy(errorMessage = message) }

    private fun validateDraftForUpdate(draft: ProfileDraft, savedDraft: ProfileDraft): ProfileFormErrors {
        val validation = validateProfileDraft(draft)
        return validation.copy(
            school = validation.school.takeIf { draft.universityIsCustom || savedDraft.university != draft.university },
            major = validation.major.takeIf { draft.majorIsCustom || savedDraft.major != draft.major }
        )
    }
}
