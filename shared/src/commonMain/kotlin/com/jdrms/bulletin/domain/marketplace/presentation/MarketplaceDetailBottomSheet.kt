package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinInputHost
import com.jdrms.bulletin.core.designsystem.ReviewSubmissionDialog
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing

data class MarketplaceDetailSheetState(
    val listing: Listing?,
    val isLoading: Boolean,
    val errorMessage: String?,
    val bookmarkErrorMessage: String?,
    val isBookmarked: Boolean,
    val currentUserId: String?,
    val hasReview: Boolean,
    val isReviewDialogOpen: Boolean = false,
    val isSubmittingReview: Boolean = false,
    val reviewErrorMessage: String? = null
)

data class MarketplaceDetailSheetActions(
    val onDismiss: () -> Unit,
    val onRetry: () -> Unit,
    val onToggleBookmark: () -> Unit,
    val onSellerClick: (sellerId: String) -> Unit = {},
    val onMessageSeller: suspend (listingId: String, sellerId: String, sellerName: String, content: String) -> Boolean =
        { _, _, _, _ -> false },
    val hasExistingConversation: suspend (listingId: String, sellerId: String) -> Boolean =
        { _, _ -> false },
    val onLeaveReview: () -> Unit = {},
    val onDismissReview: () -> Unit = {},
    val onSubmitReview: (score: Int, comment: String) -> Unit = { _, _ -> },
    val onReportListing: () -> Unit = {},
    val onSeeChat: suspend (listingId: String, sellerId: String) -> Unit = { _, _ -> }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceDetailBottomSheet(
    state: MarketplaceDetailSheetState,
    actions: MarketplaceDetailSheetActions
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = actions.onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        BulletinInputHost {
            MarketplaceDetailSheetContent(state, actions)
            if (state.isReviewDialogOpen) {
                ReviewSubmissionDialog(
                    isSubmitting = state.isSubmittingReview,
                    errorMessage = state.reviewErrorMessage,
                    onDismiss = actions.onDismissReview,
                    onSubmit = actions.onSubmitReview
                )
            }
        }
    }
}
