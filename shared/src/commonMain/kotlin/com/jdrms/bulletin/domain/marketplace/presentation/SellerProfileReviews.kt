package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.ReviewPaginationActions
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.core.designsystem.theme.bulletinColors
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceSellerReview

@Composable
internal fun SellerProfileReviews(reviews: List<MarketplaceSellerReview>) {
    var visibleReviewCount by rememberSaveable(reviews.joinToString { it.id }) {
        mutableStateOf(INITIAL_REVIEW_COUNT)
    }
    val visibleReviews = reviews.take(visibleReviewCount)
    BulletinCard {
        Column(verticalArrangement = Arrangement.spacedBy(BulletinSpacing.m)) {
            Text("Reviews", style = MaterialTheme.typography.titleMedium)
            if (reviews.isEmpty()) {
                Text(
                    text = "This seller has no reviews yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                visibleReviews.forEach { review -> SellerReview(review) }
                ReviewPaginationActions(
                    isExpanded = visibleReviewCount > INITIAL_REVIEW_COUNT,
                    hasMore = visibleReviews.size < reviews.size,
                    onSeeLess = { visibleReviewCount = INITIAL_REVIEW_COUNT },
                    onSeeMore = {
                        visibleReviewCount = nextVisibleReviewCount(visibleReviewCount, reviews.size)
                    }
                )
            }
        }
    }
}

private const val INITIAL_REVIEW_COUNT = 3
private const val FIRST_REVIEW_INCREMENT = 7
private const val SUBSEQUENT_REVIEW_INCREMENT = 10

internal fun nextVisibleReviewCount(currentCount: Int, totalCount: Int): Int {
    val increment = if (currentCount <= INITIAL_REVIEW_COUNT) {
        FIRST_REVIEW_INCREMENT
    } else {
        SUBSEQUENT_REVIEW_INCREMENT
    }
    return (currentCount + increment).coerceAtMost(totalCount)
}

@Composable
private fun SellerReview(review: MarketplaceSellerReview) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(BulletinSpacing.xs)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s)
        ) {
            Text(review.reviewerName, style = MaterialTheme.typography.labelLarge)
            Row(Modifier.semantics { contentDescription = "${review.rating} out of 5 stars" }) {
                for (value in 1..5) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = if (value <= review.rating) {
                            MaterialTheme.bulletinColors.star
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Text(
            text = review.comment,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
