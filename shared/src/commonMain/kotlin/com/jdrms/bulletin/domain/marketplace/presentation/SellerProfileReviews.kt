package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceSellerReview

@Composable
internal fun SellerProfileReviews(
    reviews: List<MarketplaceSellerReview>,
    onListingClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(BulletinSpacing.m)) {
        Text("Recent reviews", style = MaterialTheme.typography.titleMedium)
        if (reviews.isEmpty()) {
            Text(
                "This seller has no reviews yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            mostRecentSellerReviews(reviews)
                .forEach { review -> SellerReviewCard(review, onListingClick) }
        }
    }
}

internal const val RECENT_REVIEW_LIMIT = 3

internal fun mostRecentSellerReviews(
    reviews: List<MarketplaceSellerReview>
): List<MarketplaceSellerReview> = reviews.sortedByDescending { it.createdAtMillis }
    .take(RECENT_REVIEW_LIMIT)
