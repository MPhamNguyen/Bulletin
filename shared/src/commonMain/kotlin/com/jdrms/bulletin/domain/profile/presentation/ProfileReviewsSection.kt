package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.BulletinExtras
import com.jdrms.bulletin.core.designsystem.ReviewPaginationActions
import com.jdrms.bulletin.domain.profile.domain.model.StudentReputation
import com.jdrms.bulletin.domain.profile.domain.model.StudentReview

@Composable
internal fun ProfileReviewsSection(reputation: StudentReputation) {
    if (reputation.reviews.isEmpty()) return
    var visibleReviewCount by rememberSaveable(reputation.reviews.joinToString { it.id.value }) {
        mutableStateOf(INITIAL_PROFILE_REVIEW_COUNT)
    }
    val visibleReviews = reputation.reviews.take(visibleReviewCount)

    BulletinCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Reviews", style = MaterialTheme.typography.titleMedium)
            visibleReviews.forEach { review -> ProfileReview(review) }
            ReviewPaginationActions(
                isExpanded = visibleReviewCount > INITIAL_PROFILE_REVIEW_COUNT,
                hasMore = visibleReviews.size < reputation.reviews.size,
                onSeeLess = { visibleReviewCount = INITIAL_PROFILE_REVIEW_COUNT },
                onSeeMore = {
                    visibleReviewCount = nextProfileReviewCount(
                        visibleReviewCount,
                        reputation.reviews.size
                    )
                }
            )
        }
    }
}

@Composable
private fun ProfileReview(review: StudentReview) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(review.reviewerName, style = MaterialTheme.typography.labelLarge)
            Row {
                for (value in 1..5) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = if (value <= review.rating.score) {
                            BulletinExtras.colors.star
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        modifier = Modifier.padding(end = 1.dp)
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

private const val INITIAL_PROFILE_REVIEW_COUNT = 3
private const val FIRST_PROFILE_REVIEW_INCREMENT = 7
private const val LATER_PROFILE_REVIEW_INCREMENT = 10

internal fun nextProfileReviewCount(currentCount: Int, totalCount: Int): Int {
    val increment = if (currentCount <= INITIAL_PROFILE_REVIEW_COUNT) {
        FIRST_PROFILE_REVIEW_INCREMENT
    } else {
        LATER_PROFILE_REVIEW_INCREMENT
    }
    return (currentCount + increment).coerceAtMost(totalCount)
}
