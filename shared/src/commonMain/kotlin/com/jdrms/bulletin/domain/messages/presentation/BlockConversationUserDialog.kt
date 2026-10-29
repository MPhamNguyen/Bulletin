package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

@Composable
internal fun BlockConversationUserDialog(
    displayName: String,
    isBlocking: Boolean,
    errorMessage: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isBlocking) onDismiss() },
        title = { Text("Block $displayName?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)) {
                Text(
                    "You will no longer see each other's marketplace listings, and you won't be able to send " +
                        "messages to $displayName in this conversation until you unblock them."
                )
                errorMessage?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onConfirm,
                enabled = !isBlocking,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(if (isBlocking) "Blocking…" else "Block")
            }
        },
        dismissButton = { TextButton(onDismiss, enabled = !isBlocking) { Text("Cancel") } }
    )
}
