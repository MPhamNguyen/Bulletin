package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.listings.domain.model.Listing

@Composable
fun ListingDeleteConfirmationDialog(
    listing: Listing,
    isDeleting: Boolean,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        shape = MaterialTheme.shapes.large,
        title = { Text("Delete listing?") },
        text = {
            Column {
                Text(
                    "Warning: this will permanently delete \"${listing.title}\". " +
                        "This action cannot be undone."
                )
                errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(BulletinSpacing.m))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.small)
                            .padding(BulletinSpacing.m)
                    )
                }
            }
        },
        dismissButton = {
            BulletinButton(onClick = onDismiss, enabled = !isDeleting, variant = BulletinButtonVariant.Text) {
                Text("Cancel")
            }
        },
        confirmButton = {
            BulletinButton(
                onClick = onConfirm,
                enabled = !isDeleting,
                variant = BulletinButtonVariant.DestructiveText
            ) {
                Text(if (isDeleting) "Deleting…" else "Delete")
            }
        }
    )
}
