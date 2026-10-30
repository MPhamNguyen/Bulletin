package com.jdrms.bulletin.domain.marketplace.presentation

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
