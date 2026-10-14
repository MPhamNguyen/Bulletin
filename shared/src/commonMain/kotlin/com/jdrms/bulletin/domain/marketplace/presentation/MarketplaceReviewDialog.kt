package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.runtime.Composable
import com.jdrms.bulletin.core.designsystem.ReviewSubmissionDialog

@Composable
fun MarketplaceReviewDialog(
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (score: Int, comment: String) -> Unit
) {
    ReviewSubmissionDialog(isSubmitting, errorMessage, onDismiss, onSubmit)
}
