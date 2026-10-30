package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/** Asks before a category switch discards nonblank item details. Cancel leaves the form unchanged. */
@Composable
internal fun ListingCategoryChangeDialog(
    change: PendingCategoryChange,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Change category?") },
        text = {
            Text(
                "Switching to ${change.target.name} will remove these details: " +
                    change.discarded.joinToString { it.label } + "."
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Change category") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Keep category") } }
    )
}
