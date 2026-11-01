package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant

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
            BulletinButton(onClick = onConfirm, enabled = !isBlocking, variant = BulletinButtonVariant.Text) {
                Text(if (isBlocking) "Blocking…" else "Block")
            }
        },
        dismissButton = {
            BulletinButton(
                onClick = onDismiss,
                enabled = !isBlocking,
                variant = BulletinButtonVariant.Text
            ) { Text("Cancel") }
        }
    )
}
