package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
internal fun EditProfilePhoto(
    avatarUrl: String?,
    fullName: String,
    photoAction: ProfilePhotoAction?,
    isSaving: Boolean,
    onChangePhoto: () -> Unit,
    onRemovePhoto: () -> Unit
) {
    val isBusy = photoAction != null || isSaving
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(104.dp).clickable(
                enabled = !isBusy,
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
                if (photoAction != null) {
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
            TextButton(onClick = onChangePhoto, enabled = !isBusy) {
                Text(
                    if (photoAction == ProfilePhotoAction.UPLOADING) "Uploading…" else "Upload New",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            if (!avatarUrl.isNullOrBlank()) {
                Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onRemovePhoto, enabled = !isBusy) {
                    Text(
                        if (photoAction == ProfilePhotoAction.REMOVING) "Removing…" else "Remove",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
