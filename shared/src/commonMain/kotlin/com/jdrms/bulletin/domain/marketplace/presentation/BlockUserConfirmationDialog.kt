package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
internal fun BlockUserConfirmationDialog(
    isBlocking: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Block this user?") },
        text = { Text("You will no longer see each other's marketplace listings.") },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isBlocking) {
                Text(if (isBlocking) "Blocking…" else "Block")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBlocking) { Text("Cancel") }
        }
    )
}
