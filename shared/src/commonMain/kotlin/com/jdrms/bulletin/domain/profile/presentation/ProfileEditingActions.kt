package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.UpdateStudentProfile
import com.jdrms.bulletin.domain.profile.application.UploadProfilePhoto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ProfileEditingActions(
    private val uiState: MutableStateFlow<ProfileUiState>,
    private val scope: CoroutineScope,
    private val updateStudentProfile: UpdateStudentProfile,
    private val uploadProfilePhoto: UploadProfilePhoto?,
    private val cancelFlashNotification: () -> Unit,
    private val showFlashNotification: (String) -> Unit
) {
    fun onDraftChanged(profileDraft: ProfileDraft) {
        cancelFlashNotification()
        uiState.update { current ->
            current.copy(
                profileDraft = profileDraft,
                profileFormErrors = current.profileFormErrors.copy(
                    school = current.profileFormErrors.school.takeUnless {
                        profileDraft.university != current.profileDraft.university
                    },
                    major = current.profileFormErrors.major.takeUnless {
                        profileDraft.major != current.profileDraft.major
                    }
                ),
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun resetDraft() {
        val profile = uiState.value.profile ?: return
        cancelFlashNotification()
        uiState.update {
            it.copy(
                profileDraft = ProfileDraft.from(profile),
                profileFormErrors = ProfileFormErrors(),
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun updateDetails() {
        val state = uiState.value
        val profile = state.profile
        if (profile == null) {
            uiState.update { it.copy(errorMessage = "Profile is unavailable.") }
            return
        }
        val draft = state.profileDraft
        val validation = validateDraftForUpdate(draft, ProfileDraft.from(profile))
        if (!validation.isValid) {
            uiState.update { it.copy(profileFormErrors = validation) }
            return
        }

        scope.launch {
            uiState.update {
                it.copy(
                    isLoading = true,
                    profileFormErrors = ProfileFormErrors(),
                    errorMessage = null,
                    successMessage = null
                )
            }
            when (
                val result = updateStudentProfile(
                    profile = profile,
                    fullName = draft.fullName,
                    major = draft.major,
                    university = draft.university,
                    bio = draft.bio
                )
            ) {
                is Result.Success -> {
                    uiState.update {
                        it.copy(
                            profile = result.data,
                            profileDraft = ProfileDraft.from(result.data),
                            profileFormErrors = ProfileFormErrors(),
                            isEditingProfile = false,
                            activeSubscreen = ProfileSubscreen.PROFILE,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                    showFlashNotification("Profile updated")
                }
                is Result.Error -> uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exception.message ?: "Failed to update profile"
                    )
                }
            }
        }
    }

    fun uploadPhoto(bytes: ByteArray, mediaType: String) {
        val profile = uiState.value.profile
        if (profile == null) {
            uiState.update { it.copy(errorMessage = "Profile is unavailable.") }
            return
        }
        val upload = uploadProfilePhoto
        if (upload == null) {
            uiState.update { it.copy(errorMessage = "Profile photo uploads are unavailable.") }
            return
        }
        if (uiState.value.isPhotoUploading) return

        uiState.update {
            it.copy(isPhotoUploading = true, errorMessage = null, successMessage = null)
        }
        scope.launch {
            when (val result = upload(profile, bytes, mediaType)) {
                is Result.Success -> {
                    uiState.update {
                        it.copy(
                            profile = result.data,
                            profileDraft = ProfileDraft.from(result.data),
                            isPhotoUploading = false,
                            errorMessage = null
                        )
                    }
                    showFlashNotification("Profile photo updated")
                }
                is Result.Error -> uiState.update {
                    it.copy(
                        isPhotoUploading = false,
                        errorMessage = result.exception.message ?: "Failed to upload profile photo"
                    )
                }
            }
        }
    }

    fun onPhotoSelectionError(message: String) {
        uiState.update { it.copy(errorMessage = message, successMessage = null) }
    }

    fun resetRegistration() {
        if (uiState.value.isLoading) return
        cancelFlashNotification()
        uiState.update {
            it.copy(
                isAccountCreated = false,
                pendingRegistration = null,
                verifiedEmailAwaitingProfile = null,
                isEditingProfile = false,
                successMessage = null,
                errorMessage = null
            )
        }
    }

    private fun validateDraftForUpdate(
        draft: ProfileDraft,
        savedDraft: ProfileDraft
    ): ProfileFormErrors {
        val validation = validateProfileDraft(draft)
        return validation.copy(
            school = validation.school.takeIf {
                draft.universityIsCustom || savedDraft.university != draft.university
            },
            major = validation.major.takeIf {
                draft.majorIsCustom || savedDraft.major != draft.major
            }
        )
    }
}
