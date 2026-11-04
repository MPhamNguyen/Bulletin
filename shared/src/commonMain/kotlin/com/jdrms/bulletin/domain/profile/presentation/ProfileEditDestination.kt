package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.runtime.Composable
import com.jdrms.bulletin.app.navigation.ProfileDestination

@Composable
internal fun ProfileEditDestination(
    state: EditProfileUiState,
    flows: ProfileScreenFlows,
    callbacks: ProfileScreenCallbacks
) {
    val photoPicker = rememberProfilePhotoPicker(
        onPhotoSelected = flows.editing::uploadPhoto,
        onError = flows.editing::onPhotoSelectionError
    )
    EditProfileView(
        uiState = state,
        actions = EditProfileActions(
            onCancel = callbacks.onBack,
            onDraftChanged = flows.editing::onDraftChanged,
            onUpdate = { flows.editing.save { callbacks.onNavigate(ProfileDestination.PROFILE) } },
            onChangePhoto = photoPicker,
            onRemovePhoto = flows.editing::removePhoto,
            onSignOut = { flows.account.signOut(callbacks.onSignOut) }
        )
    )
}
