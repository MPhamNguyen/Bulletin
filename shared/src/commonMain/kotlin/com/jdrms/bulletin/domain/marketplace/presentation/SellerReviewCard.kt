package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.core.designsystem.theme.bulletinColors
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceSellerReview
import kotlin.time.Clock

@Composable
internal fun SellerReviewCard(
    review: MarketplaceSellerReview,
    onListingClick: ((String) -> Unit)? = null
) {
    BulletinCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(BulletinSpacing.xs)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ReviewerAvatar(review)
                Spacer(Modifier.width(BulletinSpacing.s))
                Text(
                    review.reviewerName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    review.createdAtMillis.formatReviewAge(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.semanticsRating(review.rating),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (value in 1..5) {
                    val starTint = when {
                        value <= review.rating -> MaterialTheme.bulletinColors.star
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = starTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                review.comment,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            review.relatedListingId?.let { listingId ->
                val listingModifier = onListingClick?.let { callback ->
                    Modifier.clickable { callback(listingId) }
                } ?: Modifier
                Text(
                    text = "Marketplace purchase: view listing",
                    modifier = listingModifier,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ReviewerAvatar(review: MarketplaceSellerReview) {
    val avatarUrl = review.reviewerAvatarUrl
    if (avatarUrl != null) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = "${review.reviewerName} profile photo",
            modifier = Modifier.size(32.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = review.reviewerName.firstOrNull()?.uppercase() ?: "?",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

private fun Long.formatReviewAge(): String {
    if (this <= 0L) return "Recently"
    val elapsedDays = ((Clock.System.now().toEpochMilliseconds() - this).coerceAtLeast(0L) / MILLIS_PER_DAY)
    return when {
        elapsedDays == 0L -> "Today"
        elapsedDays == 1L -> "1d ago"
        elapsedDays < DAYS_PER_WEEK -> "${elapsedDays}d ago"
        elapsedDays / DAYS_PER_WEEK == 1L -> "1w ago"
        else -> "${elapsedDays / DAYS_PER_WEEK}w ago"
    }
}

private const val MILLIS_PER_DAY = 86_400_000L
private const val DAYS_PER_WEEK = 7L

private fun Modifier.semanticsRating(rating: Int): Modifier = semantics {
    contentDescription = "$rating out of 5 stars"
}
