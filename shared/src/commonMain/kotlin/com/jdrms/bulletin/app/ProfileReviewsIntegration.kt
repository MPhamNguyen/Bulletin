package com.jdrms.bulletin.app

import androidx.compose.runtime.Composable
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceSellerReview
import com.jdrms.bulletin.domain.marketplace.presentation.SellerProfileReviews
import com.jdrms.bulletin.domain.profile.presentation.ProfileUiState

@Composable
internal fun ProfileReviewsIntegration(
    profileState: ProfileUiState,
    onViewAllReviews: () -> Unit,
    onListingClick: (String) -> Unit
) {
    if (profileState.hasLoadedOverview) {
        SellerProfileReviews(
            reviews = profileState.reputation?.reviews.orEmpty().map { review ->
                MarketplaceSellerReview(
                    id = review.id.value,
                    reviewerName = review.reviewerName,
                    rating = review.rating.score,
                    comment = review.comment,
                    createdAtMillis = review.createdAtMillis,
                    reviewerAvatarUrl = review.reviewerAvatarUrl,
                    relatedListingId = review.relatedListingId
                )
            },
            reviewLoadError = null,
            isLoadingReviews = false,
            onRetryReviews = {},
            onViewAllReviews = onViewAllReviews,
            onListingClick = onListingClick
        )
    }
}
