package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceSellerReview

@Composable
internal fun SellerProfileReviews(
    reviews: List<MarketplaceSellerReview>,
    reviewLoadError: String?,
    isLoadingReviews: Boolean,
    onRetryReviews: () -> Unit,
    onViewAllReviews: () -> Unit,
    onListingClick: (String) -> Unit
) {
    BulletinCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(BulletinSpacing.m)) {
            Text("Recent reviews", style = MaterialTheme.typography.titleMedium)
            if (reviewLoadError != null) {
                Text(
                    reviewLoadError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                BulletinButton(
                    onClick = onRetryReviews,
                    enabled = !isLoadingReviews,
                    variant = BulletinButtonVariant.Text
                ) {
                    Text(if (isLoadingReviews) "Loading reviews…" else "Try again")
                }
            } else if (reviews.isEmpty()) {
                Text(
                    "This seller has no reviews yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                mostRecentSellerReviews(reviews)
                    .forEach { review -> SellerReviewCard(review, onListingClick) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BulletinButton(
                        onClick = onViewAllReviews,
                        variant = BulletinButtonVariant.Text
                    ) {
                        Text("View All Reviews")
                    }
                }
            }
        }
    }
}

internal const val RECENT_REVIEW_LIMIT = 3

internal fun mostRecentSellerReviews(
    reviews: List<MarketplaceSellerReview>
): List<MarketplaceSellerReview> = reviews.sortedByDescending { it.createdAtMillis }
    .take(RECENT_REVIEW_LIMIT)
