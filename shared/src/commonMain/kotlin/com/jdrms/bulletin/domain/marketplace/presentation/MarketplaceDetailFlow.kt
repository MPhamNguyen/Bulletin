package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal fun MarketplaceDetailFlow(
    state: MarketplaceUiState,
    viewModel: MarketplaceViewModel,
    sellerProfileViewModel: SellerProfileViewModel,
    listingReportViewModel: ListingReportViewModel,
    actions: MarketplaceScreenActions,
    navigation: MarketplaceDetailNavigation,
    zoomOrigin: ListingZoomOrigin? = null
) {
    val isOpen = state.detail.isOpen
    val session = rememberLastOpenSession(
        isOpen = isOpen,
        live = ListingZoomSession(
            sheet = state.toDetailSheetState(),
            origin = zoomOrigin?.takeIf { it.listingId == state.detail.listingId }
        )
    )
    // Closing clears the detail state at once; the page keeps showing its last content while it zooms back.
    ListingZoomOverlay(isOpen = isOpen, origin = session.origin) { photoModifier ->
        ListingDetailsPage(
            state = session.sheet,
            actions = detailActions(
                state = state,
                viewModel = viewModel,
                sellerProfileViewModel = sellerProfileViewModel,
                listingReportViewModel = listingReportViewModel,
                actions = actions,
                onNavigate = navigation.onNavigate,
                onDismiss = if (isOpen) navigation.onDismiss else ({})
            ),
            photoModifier = photoModifier
        )
    }
    if (isOpen) {
        MarketplaceListingReportFlow(listingReportViewModel) {
            listingReportViewModel.dismiss()
            navigation.onDismiss()
            viewModel.refreshListings()
        }
    }
}

private class ListingZoomSession(val sheet: MarketplaceDetailSheetState, val origin: ListingZoomOrigin?)

private class LastOpenSession(var session: ListingZoomSession)

@Composable
private fun rememberLastOpenSession(isOpen: Boolean, live: ListingZoomSession): ListingZoomSession {
    val last = remember { LastOpenSession(live) }
    if (isOpen) last.session = live
    return last.session
}

private fun MarketplaceUiState.toDetailSheetState() = MarketplaceDetailSheetState(
    listing = detail.listing,
    preview = detail.preview,
    isLoading = detail.isLoading,
    errorMessage = detail.errorMessage,
    bookmarkErrorMessage = bookmarkErrorMessage,
    isBookmarked = (detail.listing?.id ?: detail.preview?.id) in bookmarkedItemIds,
    currentUserId = currentUserId,
    hasReview = detail.hasReview,
    isReviewDialogOpen = detail.isReviewDialogOpen,
    isSubmittingReview = detail.isSubmittingReview,
    reviewErrorMessage = detail.reviewErrorMessage,
    previewPhotoUrl = items.firstOrNull { it.id.value == detail.listingId }?.photos?.firstOrNull()
)

private fun detailActions(
    state: MarketplaceUiState,
    viewModel: MarketplaceViewModel,
    sellerProfileViewModel: SellerProfileViewModel,
    listingReportViewModel: ListingReportViewModel,
    actions: MarketplaceScreenActions,
    onNavigate: (MarketplaceDestination) -> Unit,
    onDismiss: () -> Unit
) = MarketplaceDetailSheetActions(
    onDismiss = onDismiss,
    onRetry = viewModel::retryLoadListingDetail,
    onToggleBookmark = {
        (state.detail.listing?.id ?: state.detail.preview?.id)?.let { viewModel.toggleBookmark(it) }
    },
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
