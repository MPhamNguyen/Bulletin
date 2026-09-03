package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ModeEdit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import bulletin.shared.generated.resources.Res
import bulletin.shared.generated.resources.ic_arrow_back
import bulletin.shared.generated.resources.ic_graduation_cap
import com.jdrms.bulletin.core.designsystem.BulletinButtonDefaults
import com.jdrms.bulletin.core.designsystem.BulletinExtras
import com.jdrms.bulletin.core.designsystem.BulletinInactiveButtonDefaults
import com.jdrms.bulletin.core.designsystem.BulletinTextFieldDefaults
import org.jetbrains.compose.resources.painterResource

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            ProfileTopBar(
                title = "Edit Profile",
                onBackClick = {
                    viewModel.clearMessages()
                    onBack()
                },
                actionLabel = "Cancel",
                onActionClick = viewModel::resetProfileDraft
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ProfileContent(
                    uiState = uiState,
                    onDraftChanged = viewModel::onProfileDraftChanged,
                    onUpdate = viewModel::updateProfileDetails,
                    onSignOut = onSignOut
                )
            }
        }

        AnimatedContent(
            targetState = uiState.successMessage,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 72.dp, start = 24.dp, end = 24.dp)
                .zIndex(1f),
            transitionSpec = {
                (slideInVertically(initialOffsetY = { -it }) + fadeIn()) togetherWith
                    (slideOutVertically(targetOffsetY = { -it }) + fadeOut())
            },
            label = "Profile flash notification"
        ) { message ->
            if (message != null) ProfileUpdateMessage(message)
        }
    }
}

@Composable
private fun ProfileTopBar(
    title: String,
    onBackClick: () -> Unit,
    actionLabel: String?,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                painter = painterResource(Res.drawable.ic_arrow_back),
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .padding(start = if (actionLabel == null) 0.dp else 48.dp)
        )
        if (actionLabel != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    onDraftChanged: (ProfileDraft) -> Unit,
    onUpdate: () -> Unit,
    onSignOut: () -> Unit = {}
) {
    EditProfilePhoto()

    Spacer(modifier = Modifier.height(28.dp))
    ProfileEditField(
        label = "Full Name",
        value = uiState.profileDraft.fullName,
        onValueChange = { onDraftChanged(uiState.profileDraft.copy(fullName = it)) }
    )
    Spacer(modifier = Modifier.height(18.dp))
    ProfileEditField(
        label = "Major",
        value = uiState.profileDraft.major,
        placeholder = "Your major",
        onValueChange = { onDraftChanged(uiState.profileDraft.copy(major = it)) }
    )
    Spacer(modifier = Modifier.height(18.dp))
    ProfileEditField(
        label = "School",
        value = uiState.profileDraft.university,
        onValueChange = { onDraftChanged(uiState.profileDraft.copy(university = it)) }
    )
    Spacer(modifier = Modifier.height(18.dp))
    ProfileEditField(
        label = "Bio",
        value = uiState.profileDraft.bio,
        placeholder = "Tell us a bit about yourself...",
        singleLine = false,
        onValueChange = { onDraftChanged(uiState.profileDraft.copy(bio = it)) }
    )

    uiState.errorMessage?.let { error ->
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }

    val isModified = uiState.isProfileModified
    val isButtonEnabled = isModified && !uiState.isLoading

    Spacer(modifier = Modifier.height(40.dp))
    Button(
        onClick = onUpdate,
        enabled = isButtonEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = MaterialTheme.shapes.medium,
        colors = if (isModified) {
            BulletinButtonDefaults.buttonColors()
        } else {
            BulletinInactiveButtonDefaults.colors()
        }
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(text = "Update", style = MaterialTheme.typography.titleLarge)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedButton(
        onClick = onSignOut,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = MaterialTheme.shapes.medium,
        border = BulletinButtonDefaults.destructiveOutlinedButtonBorder(),
        colors = BulletinButtonDefaults.destructiveOutlinedButtonColors()
    ) {
        Text(
            text = "Sign Out",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
    }

    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun ProfileUpdateMessage(message: String) {
    Row(
        modifier = Modifier
            .shadow(6.dp, MaterialTheme.shapes.large)
            .background(
                color = BulletinExtras.colors.successContainer,
                shape = MaterialTheme.shapes.large
            )
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = BulletinExtras.colors.success
        )
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = BulletinExtras.colors.onSuccessContainer
        )
    }
}

@Composable
private fun EditProfilePhoto() {
    Box(
        modifier = Modifier.size(124.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_graduation_cap),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(44.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.CameraAlt,
                contentDescription = "Change profile photo",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
    Text(
        text = "Change Photo",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun ProfileEditField(
    label: String,
    value: String,
    placeholder: String = "",
    singleLine: Boolean = true,
    onValueChange: (String) -> Unit
) {
    var isEditing by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        readOnly = !isEditing,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 5,
        maxLines = if (singleLine) 1 else 5,
        trailingIcon = {
            IconButton(
                onClick = {
                    val nextState = !isEditing
                    isEditing = nextState
                    if (nextState) {
                        focusRequester.requestFocus()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.ModeEdit,
                    contentDescription = if (isEditing) "Done editing $label" else "Edit $label",
                    tint = if (isEditing) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        },
        shape = MaterialTheme.shapes.medium,
        colors = BulletinTextFieldDefaults.colors(),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
    )
}
