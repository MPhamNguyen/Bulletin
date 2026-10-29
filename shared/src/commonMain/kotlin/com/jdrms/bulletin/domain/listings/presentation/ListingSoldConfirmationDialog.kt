package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.core.designsystem.BulletinSpacing
import com.jdrms.bulletin.domain.listings.application.RecentListingBuyer
import com.jdrms.bulletin.domain.listings.domain.model.Listing

data class ListingSoldConfirmationState(
    val listing: Listing,
    val isMarkingSold: Boolean,
    val recentBuyers: List<RecentListingBuyer> = emptyList(),
    val selectedBuyerId: String? = null,
    val soldOnAnotherPlatform: Boolean = false,
    val errorMessage: String? = null
)

data class ListingSoldConfirmationActions(
    val onDismiss: () -> Unit,
    val onConfirm: () -> Unit,
    val onBuyerSelected: (String) -> Unit = {},
    val onSoldOnAnotherPlatform: () -> Unit = {}
)

@Composable
fun ListingSoldConfirmationDialog(
    state: ListingSoldConfirmationState,
    actions: ListingSoldConfirmationActions
) {
    AlertDialog(
        onDismissRequest = { if (!state.isMarkingSold) actions.onDismiss() },
        shape = MaterialTheme.shapes.large,
        title = { Text("Mark listing as sold?") },
        text = {
            Column {
                Text("${state.listing.title} will be removed from marketplace results.")
                Spacer(modifier = Modifier.height(BulletinSpacing.m))
                Text("Who did you sell it to?", style = MaterialTheme.typography.titleSmall)
                for (buyer in state.recentBuyers) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = state.selectedBuyerId == buyer.id,
                            onClick = { actions.onBuyerSelected(buyer.id) },
                            enabled = !state.isMarkingSold
                        )
                        Text(buyer.displayName)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = state.soldOnAnotherPlatform,
                        onClick = actions.onSoldOnAnotherPlatform,
                        enabled = !state.isMarkingSold
                    )
                    Text("Sold on another platform")
                }
                state.errorMessage?.let { error ->
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
            TextButton(onClick = actions.onDismiss, enabled = !state.isMarkingSold) {
                Text("Cancel")
            }
        },
        confirmButton = {
            TextButton(onClick = actions.onConfirm, enabled = !state.isMarkingSold) {
                Text(if (state.isMarkingSold) "Marking as sold..." else "Mark as sold")
            }
        }
    )
}
