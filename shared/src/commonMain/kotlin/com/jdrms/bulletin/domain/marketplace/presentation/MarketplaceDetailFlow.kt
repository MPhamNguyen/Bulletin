package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.runtime.Composable

@Composable
internal fun MarketplaceDetailFlow(
    state: MarketplaceUiState,
    viewModel: MarketplaceViewModel,
    sellerProfileViewModel: SellerProfileViewModel,
    listingReportViewModel: ListingReportViewModel,
    actions: MarketplaceScreenActions,
    onNavigate: (MarketplaceDestination) -> Unit,
    onDetailDismiss: () -> Unit
) {
    if (!state.detail.isOpen) return
    MarketplaceDetailOverlay(
        state = state,
        viewModel = viewModel,
        sellerProfileViewModel = sellerProfileViewModel,
        listingReportViewModel = listingReportViewModel,
        actions = actions,
        onNavigate = onNavigate,
        onDetailDismiss = onDetailDismiss
    )
    MarketplaceListingReportFlow(listingReportViewModel) {
        listingReportViewModel.dismiss()
        onDetailDismiss()
        viewModel.refreshListings()
    }
}

@Composable
private fun MarketplaceDetailOverlay(
    state: MarketplaceUiState,
    viewModel: MarketplaceViewModel,
    sellerProfileViewModel: SellerProfileViewModel,
    listingReportViewModel: ListingReportViewModel,
    actions: MarketplaceScreenActions,
    onNavigate: (MarketplaceDestination) -> Unit,
    onDetailDismiss: () -> Unit
) {
    MarketplaceDetailBottomSheet(
        state = MarketplaceDetailSheetState(
            listing = state.detail.listing,
            isLoading = state.detail.isLoading,
            errorMessage = state.detail.errorMessage,
            bookmarkErrorMessage = state.bookmarkErrorMessage,
            isBookmarked = state.detail.listing?.let { it.id in state.bookmarkedItemIds } ?: false,
            currentUserId = state.currentUserId,
            hasReview = state.detail.hasReview,
            isReviewDialogOpen = state.detail.isReviewDialogOpen,
            isSubmittingReview = state.detail.isSubmittingReview,
            reviewErrorMessage = state.detail.reviewErrorMessage
        ),
        actions = MarketplaceDetailSheetActions(
            onDismiss = onDetailDismiss,
            onRetry = viewModel::retryLoadListingDetail,
            onToggleBookmark = { state.detail.listing?.let { viewModel.toggleBookmark(it.id) } },
            onLeaveReview = viewModel.reviewActions::openReviewDialog,
            onDismissReview = viewModel.reviewActions::dismissReviewDialog,
            onSubmitReview = viewModel.reviewActions::submitReview,
            onReportListing = { beginMarketplaceListingReport(listingReportViewModel, state.detail.listing) },
            onSellerClick = { sellerId ->
                sellerProfileViewModel.load(sellerId)
                onNavigate(MarketplaceDestination.SELLER_PROFILE)
            },
            onMessageSeller = actions.onMessageSeller,
            onMessageSent = actions.onMessageSent,
            hasExistingConversation = actions.hasExistingConversation,
            onSeeChat = actions.onSeeChat
        )
    )
}
