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
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.application.RecentListingBuyer

@Composable
fun ListingSoldConfirmationDialog(
    listing: Listing,
    isMarkingSold: Boolean,
    recentBuyers: List<RecentListingBuyer> = emptyList(),
    selectedBuyerId: String? = null,
    soldOnAnotherPlatform: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onBuyerSelected: (String) -> Unit = {},
    onSoldOnAnotherPlatform: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = { if (!isMarkingSold) onDismiss() },
        shape = MaterialTheme.shapes.large,
        title = { Text("Mark listing as sold?") },
        text = {
            Column {
                Text("${listing.title} will be removed from marketplace results.")
                Spacer(modifier = Modifier.height(12.dp))
                Text("Who did you sell it to?", style = MaterialTheme.typography.titleSmall)
                recentBuyers.forEach { buyer ->
                    androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedBuyerId == buyer.id,
                            onClick = { onBuyerSelected(buyer.id) },
                            enabled = !isMarkingSold
                        )
                        Text(buyer.displayName)
                    }
                }
                androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(
                        selected = soldOnAnotherPlatform,
                        onClick = onSoldOnAnotherPlatform,
                        enabled = !isMarkingSold
                    )
                    Text("Sold on another platform")
                }
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
                Text(if (isMarkingSold) "Marking as sold..." else "Mark as sold")
            }
        }
    )
}
