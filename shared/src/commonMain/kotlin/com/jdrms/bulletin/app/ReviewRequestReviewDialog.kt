package com.jdrms.bulletin.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.BulletinFieldDecoration
import com.jdrms.bulletin.core.designsystem.BulletinFieldOptions
import com.jdrms.bulletin.core.designsystem.BulletinTextField
import com.jdrms.bulletin.core.designsystem.ReviewRatingPicker
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinElevation
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

@Composable
internal fun ReviewRequestReviewDialog(
    state: ReviewRequestDialogState,
    onSubmit: (Int, String) -> Unit,
    onBack: () -> Unit
) {
    var score by remember(state.request) { mutableStateOf(0) }
    var comment by remember(state.request) { mutableStateOf("") }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = BulletinElevation.overlay,
            shadowElevation = BulletinElevation.modal
        ) {
            Column(
                modifier = Modifier.padding(BulletinSpacing.l),
                verticalArrangement = Arrangement.spacedBy(BulletinSpacing.m)
            ) {
                Text(
                    "Review the seller",
                    style = MaterialTheme.typography.titleLarge
                )
                if (state.queueSize > 1) {
                    Text(
                        "Request ${state.queuePosition} of ${state.queueSize}",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Text(
                    "Share your experience with the seller for ${state.request.listingTitle}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ReviewRequestListingPreview(state.request.listingTitle, state.listingImageUrl)
                ReviewRatingPicker(score, onScoreChange = { score = it }, enabled = !state.isProcessing)
                BulletinTextField(
                    value = comment,
                    onValueChange = { comment = it.take(MAX_REVIEW_COMMENT_LENGTH) },
                    modifier = Modifier.fillMaxWidth(),
                    decoration = BulletinFieldDecoration(
                        label = { Text("Comment") },
                        placeholder = { Text("Describe your experience") }
                    ),
                    options = BulletinFieldOptions(minLines = 3, maxLines = 5)
                )
                state.failure?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    BulletinButton(
                        onClick = onBack,
                        enabled = !state.isProcessing,
                        variant = BulletinButtonVariant.Text
                    ) {
                        Text("Back")
                    }
                    BulletinButton(
                        onClick = { onSubmit(score, comment) },
                        enabled = !state.isProcessing && score in 1..5 && comment.isNotBlank()
                    ) {
                        Text(if (state.isProcessing) "Submitting…" else "Submit review")
                    }
                }
            }
        }
    }
}

private const val MAX_REVIEW_COMMENT_LENGTH = 500
