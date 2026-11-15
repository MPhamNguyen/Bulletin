package com.jdrms.bulletin.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequest
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestStatus

internal data class ReviewRequestDialogState(
    val request: ReviewRequest,
    val queuePosition: Int,
    val queueSize: Int,
    val listingImageUrl: String?,
    val listingSellerName: String,
    val failure: String?,
    val isProcessing: Boolean
)

internal data class ReviewRequestDialogActions(
    val onAccept: () -> Unit,
    val onDecline: () -> Unit,
    val onSubmit: (Int, String) -> Unit,
    val onDefer: () -> Unit
)

@Composable
internal fun ReviewRequestDialog(state: ReviewRequestDialogState, actions: ReviewRequestDialogActions) {
    if (state.request.status == ReviewRequestStatus.PENDING) {
        PendingReviewRequestDialog(state, actions)
    } else {
        ReviewRequestReviewDialog(state, actions.onSubmit, actions.onDefer)
    }
}

@Composable
private fun PendingReviewRequestDialog(state: ReviewRequestDialogState, actions: ReviewRequestDialogActions) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.9f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(BulletinSpacing.l),
                verticalArrangement = Arrangement.spacedBy(BulletinSpacing.m)
            ) {
                Text("Review your purchase", style = MaterialTheme.typography.titleLarge)
                if (state.queueSize > 1) {
                    Text(
                        "Request ${state.queuePosition} of ${state.queueSize}",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Text("Would you like to review the seller?")
                ReviewRequestPurchasePreview(
                    title = state.request.listingTitle,
                    sellerName = state.listingSellerName,
                    imageUrl = state.listingImageUrl,
                    modifier = Modifier.weight(1f)
                )
                state.failure?.let { Text(it) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BulletinButton(onClick = actions.onDecline, enabled = !state.isProcessing) {
                        Text("Decline")
                    }
                    BulletinButton(onClick = actions.onAccept, enabled = !state.isProcessing) {
                        Text("Accept")
                    }
                }
            }
        }
    }
}
