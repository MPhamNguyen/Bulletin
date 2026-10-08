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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.domain.listings.domain.model.Listing

@Composable
fun ListingSoldConfirmationDialog(
    listing: Listing,
    isMarkingSold: Boolean,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isMarkingSold) onDismiss() },
        shape = MaterialTheme.shapes.large,
        title = { Text("Mark listing as sold?") },
        text = {
            Column {
                Text("${listing.title} will be removed from marketplace results.")
                errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isMarkingSold) {
                Text("Cancel")
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isMarkingSold) {
                Text(if (isMarkingSold) "Marking as sold…" else "Mark as sold")
            }
        }
    )
}
