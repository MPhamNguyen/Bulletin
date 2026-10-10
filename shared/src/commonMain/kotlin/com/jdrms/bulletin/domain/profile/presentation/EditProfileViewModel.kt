package com.jdrms.bulletin.domain.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.common.UserMessenger
import com.jdrms.bulletin.domain.profile.application.SessionRepository
import com.jdrms.bulletin.domain.profile.application.SessionState
import com.jdrms.bulletin.domain.profile.application.UpdateStudentProfile
import com.jdrms.bulletin.domain.profile.application.UploadProfilePhoto
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditProfileUiState(
    val profile: StudentProfile? = null,
    val profileDraft: ProfileDraft = ProfileDraft(),
    val profileFormErrors: ProfileFormErrors = ProfileFormErrors(),
    val isLoading: Boolean = false,
    val isPhotoUploading: Boolean = false,
    val errorMessage: String? = null
) {
    val isProfileModified: Boolean get() = profile != null && ProfileDraft.from(profile) != profileDraft
}

class EditProfileViewModel(
    private val sessionRepository: SessionRepository,
    private val updateStudentProfile: UpdateStudentProfile,
    private val uploadProfilePhoto: UploadProfilePhoto,
    private val messenger: UserMessenger
) : ViewModel() {
    private val state = MutableStateFlow(EditProfileUiState())
    val uiState = state.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.state.collect { session ->
                val profile = (session as? SessionState.Authenticated)?.profile
                state.update {
                    it.copy(profile = profile, profileDraft = profile?.let(ProfileDraft::from) ?: ProfileDraft())
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
                    state.update { it.copy(isLoading = false) }
                    messenger.show("Profile updated")
                    onSaved()
                }
                is Result.Error -> state.update {
                    it.copy(isLoading = false, errorMessage = result.exception.message ?: "Failed to update profile")
                }
            }
        }
    }

    fun uploadPhoto(bytes: ByteArray, mediaType: String) {
        val profile = state.value.profile ?: run {
            state.update { it.copy(errorMessage = "Profile is unavailable.") }
            return
        }
        if (state.value.isPhotoUploading) return
        state.update { it.copy(isPhotoUploading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = uploadProfilePhoto(profile, bytes, mediaType)) {
                is Result.Success -> {
                    sessionRepository.authenticated(result.data)
                    state.update { it.copy(isPhotoUploading = false) }
                    messenger.show("Profile photo updated")
                }
                is Result.Error -> state.update {
                    it.copy(
                        isPhotoUploading = false,
                        errorMessage = result.exception.message ?: "Failed to upload profile photo"
                    )
                }
            }
        }
    }

    fun onPhotoSelectionError(message: String) = state.update { it.copy(errorMessage = message) }

    private fun validateDraftForUpdate(draft: ProfileDraft, savedDraft: ProfileDraft): ProfileFormErrors {
        val validation = validateProfileDraft(draft)
        return validation.copy(
            school = validation.school.takeIf { draft.universityIsCustom || savedDraft.university != draft.university },
            major = validation.major.takeIf { draft.majorIsCustom || savedDraft.major != draft.major }
        )
    }
}
