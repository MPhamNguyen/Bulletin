package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinButtonDefaults
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.BulletinTextFieldDefaults

internal const val BIO_MAX_LENGTH = 160
internal const val BIO_WARN_LENGTH = 150
internal const val OTHER_LABEL = "Other"

@Composable
internal fun EditProfileView(
    uiState: ProfileUiState,
    onCancel: () -> Unit,
    onDraftChanged: (ProfileDraft) -> Unit,
    onUpdate: () -> Unit,
    onChangePhoto: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onCancel) {
                Text(
                    "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Edit Profile", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Student Marketplace",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EditProfileBody(
                uiState = uiState,
                onDraftChanged = onDraftChanged,
                onSave = onUpdate,
                onChangePhoto = onChangePhoto,
                onSignOut = onSignOut
            )
        }
    }
}

@Composable
private fun EditProfileBody(
    uiState: ProfileUiState,
    onDraftChanged: (ProfileDraft) -> Unit,
    onSave: () -> Unit,
    onChangePhoto: () -> Unit,
    onSignOut: () -> Unit
) {
    val draft = uiState.profileDraft

    EditProfilePhoto(
        avatarUrl = uiState.profile?.avatarUrl,
        fullName = uiState.profile?.fullName.orEmpty(),
        isUploading = uiState.isPhotoUploading,
        onChangePhoto = onChangePhoto
    )
    Spacer(Modifier.height(20.dp))

    ProfileEditCard("Personal Info") {
        ProfileEditField(
            label = "Full Name",
            value = draft.fullName,
            leadingIcon = Icons.Outlined.Person,
            trailingAction = if (draft.fullName.isNotEmpty()) {
                {
                    IconButton(onClick = { onDraftChanged(draft.copy(fullName = "")) }) {
                        Icon(
                            Icons.Filled.Cancel,
                            contentDescription = "Clear name",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                null
            },
            onValueChange = { onDraftChanged(draft.copy(fullName = it)) }
        )
        Spacer(Modifier.height(14.dp))
        BioField(
            value = draft.bio,
            onValueChange = { onDraftChanged(draft.copy(bio = it)) }
        )
    }

    Spacer(Modifier.height(16.dp))
    ProfileEditCard(
        title = "Campus & Academics",
        trailingContent = if (uiState.profile?.isVerified == true) {
            { VerifiedLabel() }
        } else {
            null
        }
    ) {
        ProfileComboField(
            spec = ComboFieldSpec(
                label = "School",
                value = draft.university,
                placeholder = "Search or select your school",
                leadingIcon = Icons.Outlined.School,
                items = SCHOOL_OPTIONS,
                isOther = draft.universityIsCustom ||
                    (draft.university.isNotBlank() && !SCHOOL_OPTIONS.isKnownOption(draft.university)),
                otherLabel = "Your school's full name",
                otherPlaceholder = "e.g. Lincoln University",
                errorMessage = uiState.profileFormErrors.school
            ),
            onValueChange = {
                onDraftChanged(draft.copy(university = it, universityIsCustom = false))
            },
            onOtherSelected = {
                onDraftChanged(draft.copy(university = "", universityIsCustom = true))
            },
            onOtherValueChange = { onDraftChanged(draft.copy(university = it, universityIsCustom = true)) }
        )
        Spacer(Modifier.height(14.dp))
        ProfileComboField(
            spec = ComboFieldSpec(
                label = "Major / Department",
                value = draft.major,
                placeholder = "Search or select your major",
                leadingIcon = Icons.AutoMirrored.Outlined.MenuBook,
                items = MAJOR_OPTIONS,
                isOther = draft.majorIsCustom ||
                    (draft.major.isNotBlank() && !MAJOR_OPTIONS.isKnownOption(draft.major)),
                otherLabel = "Your major or department",
                otherPlaceholder = "e.g. Marine Biology",
                errorMessage = uiState.profileFormErrors.major
            ),
            onValueChange = {
                onDraftChanged(draft.copy(major = it, majorIsCustom = false))
            },
            onOtherSelected = {
                onDraftChanged(draft.copy(major = "", majorIsCustom = true))
            },
            onOtherValueChange = { onDraftChanged(draft.copy(major = it, majorIsCustom = true)) }
        )
    }

    uiState.errorMessage?.let {
        Spacer(Modifier.height(12.dp))
        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = onSave,
        enabled = uiState.isProfileModified && !uiState.isLoading,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = MaterialTheme.shapes.medium,
        colors = BulletinButtonDefaults.buttonColors()
    ) {
        Text("Save Changes", style = MaterialTheme.typography.labelLarge)
    }
    Spacer(Modifier.height(16.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Spacer(Modifier.height(16.dp))
    SignOutButton(onClick = onSignOut)
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun ProfileEditCard(
    title: String,
    trailingContent: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    BulletinCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            trailingContent?.invoke()
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun VerifiedLabel() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Outlined.Verified,
            contentDescription = null,
            modifier = Modifier.size(13.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(4.dp))
        Text(
            "Verified .edu",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun EditProfilePhoto(
    avatarUrl: String?,
    fullName: String,
    isUploading: Boolean,
    onChangePhoto: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(104.dp).clickable(
                enabled = !isUploading,
                role = Role.Button,
                onClickLabel = "Change profile photo",
                onClick = onChangePhoto
            ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), MaterialTheme.shapes.extraLarge)
            ) {
                ProfileAvatarImage(avatarUrl = avatarUrl, fullName = fullName, size = 96.dp)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraLarge)
                    .border(2.dp, MaterialTheme.colorScheme.background, MaterialTheme.shapes.extraLarge),
                contentAlignment = Alignment.Center
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Outlined.AddAPhoto,
                        contentDescription = "Upload photo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onChangePhoto, enabled = !isUploading) {
                Text(
                    if (isUploading) "Uploading…" else "Upload New",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = {}, enabled = false) {
                Text(
                    "Remove",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium)
}

@Composable
private fun ProfileEditField(
    label: String,
    value: String,
    leadingIcon: ImageVector,
    placeholder: String = "",
    trailingAction: (@Composable () -> Unit)? = null,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        FieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(leadingIcon, contentDescription = null) },
            trailingIcon = trailingAction,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            shape = MaterialTheme.shapes.medium,
            colors = BulletinTextFieldDefaults.colors(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
