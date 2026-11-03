package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.listings.domain.model.Listing

@Composable
fun ListingRestoreConfirmationDialog(
    listing: Listing,
    isRestoring: Boolean,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isRestoring) onDismiss() },
        shape = MaterialTheme.shapes.large,
        title = { Text("Restore listing?") },
        text = {
            Column {
                Text("${listing.title} will be visible in marketplace results again.")
                errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(BulletinSpacing.m))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(BulletinSpacing.m)
                        )
                    }
                }
            }
        },
        dismissButton = {
            BulletinButton(onClick = onDismiss, enabled = !isRestoring, variant = BulletinButtonVariant.Text) {
                Text("Cancel")
            }
        },
        confirmButton = {
            BulletinButton(onClick = onConfirm, enabled = !isRestoring, variant = BulletinButtonVariant.Text) {
                Text(if (isRestoring) "Restoring…" else "Restore listing")
            }
        }
    )
}
