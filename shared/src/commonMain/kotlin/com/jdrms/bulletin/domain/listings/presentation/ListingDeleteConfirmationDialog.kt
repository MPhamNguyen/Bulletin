package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.jdrms.bulletin.domain.listings.domain.model.Listing

@Composable
fun ListingDeleteConfirmationDialog(
    listing: Listing,
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        shape = MaterialTheme.shapes.large,
        title = { Text("Delete listing?") },
        text = {
            Text(
                "Warning: this will permanently delete \"${listing.title}\". " +
                    "This action cannot be undone."
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text("Cancel")
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(if (isDeleting) "Deleting…" else "Delete")
            }
        }
    )
}
