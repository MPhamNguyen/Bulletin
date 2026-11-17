package com.jdrms.bulletin.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.jdrms.bulletin.domain.marketplace.presentation.SellerProfileReviews
import com.jdrms.bulletin.domain.marketplace.presentation.SellerProfileViewModel

@Composable
internal fun ProfileReviewsIntegration(
    sellerProfileViewModel: SellerProfileViewModel,
    onViewAllReviews: () -> Unit,
    onListingClick: (String) -> Unit
) {
    val state by sellerProfileViewModel.uiState.collectAsState()
    state.profile?.let { seller ->
        SellerProfileReviews(
            reviews = seller.reviews,
            reviewLoadError = state.reviewLoadError,
            isLoadingReviews = state.isLoadingMoreReviews,
            onRetryReviews = sellerProfileViewModel::loadMoreReviews,
            onViewAllReviews = onViewAllReviews,
            onListingClick = onListingClick
        )
    }
}
