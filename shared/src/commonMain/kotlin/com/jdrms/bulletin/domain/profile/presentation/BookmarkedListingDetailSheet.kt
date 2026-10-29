package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.core.common.formatRelativeTime
import com.jdrms.bulletin.core.designsystem.BulletinButtonDefaults
import com.jdrms.bulletin.core.designsystem.BulletinExtras
import com.jdrms.bulletin.core.designsystem.BulletinSpacing
import com.jdrms.bulletin.core.designsystem.MessageSellerQuickAction
import com.jdrms.bulletin.core.designsystem.MessageSellerQuickActionActions
import com.jdrms.bulletin.core.designsystem.ReviewSubmissionDialog
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListing

internal data class BookmarkedListingReviewActions(
    val isAvailable: Boolean = false,
    val onOpen: () -> Unit = {},
    val isOpen: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val onDismiss: () -> Unit = {},
    val onSubmit: (score: Int, comment: String) -> Unit = { _, _ -> }
)

internal data class BookmarkedListingDetailActions(
    val onDismiss: () -> Unit,
    val onRemoveBookmark: () -> Unit,
    val canMessageSeller: Boolean,
    val onMessageSeller: suspend (String) -> Boolean,
    val hasExistingConversation: suspend () -> Boolean,
    val onMessageSent: () -> Unit,
    val review: BookmarkedListingReviewActions = BookmarkedListingReviewActions(),
    val onSeeChat: suspend (listingId: String, sellerId: String) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BookmarkedListingDetailSheet(
    listing: ProfileBookmarkedListing,
    isRemovingBookmark: Boolean,
    errorMessage: String?,
    actions: BookmarkedListingDetailActions
) {
    ModalBottomSheet(
        onDismissRequest = actions.onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(BulletinSpacing.l)
        ) {
            BookmarkedListingPhotos(listing.photos)
            ListingTitleAndPrice(listing)
            Row(horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s)) {
                ListingBadge(listing.category, isPrimary = false)
                ListingBadge(listing.condition.replace("_", " "), isPrimary = true)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            BookmarkedListingSeller(listing.sellerName, listing.sellerReputationScore)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "About this item",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = listing.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatRelativeTime(listing.createdAtMillis, currentTimeMillis()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (actions.canMessageSeller) {
                MessageSellerQuickAction(
                    actions = MessageSellerQuickActionActions(
                        conversationKey = "${listing.id}:${listing.sellerId}",
                        hasExistingConversation = actions.hasExistingConversation,
                        onSend = actions.onMessageSeller,
                        onSent = actions.onMessageSent,
                        onSeeChat = {
                            actions.onSeeChat(listing.id, listing.sellerId)
                        }
                    )
                )
            }
            if (actions.review.isAvailable) {
                Button(
                    onClick = actions.review.onOpen,
                    modifier = Modifier.fillMaxWidth(),
                    colors = BulletinButtonDefaults.buttonColors()
                ) {
                    Text("Leave Review for Seller")
                }
            }
            errorMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            ListingDetailActions(isRemovingBookmark, actions.onRemoveBookmark, actions.onDismiss)
        }
        if (actions.review.isOpen) {
            ReviewSubmissionDialog(
                isSubmitting = actions.review.isSubmitting,
                errorMessage = actions.review.errorMessage,
                onDismiss = actions.review.onDismiss,
                onSubmit = actions.review.onSubmit
            )
        }
    }
}

@Composable
private fun ListingTitleAndPrice(listing: ProfileBookmarkedListing) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = listing.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(BulletinSpacing.m))
        Text(
            text = listing.price,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ListingDetailActions(
    isRemovingBookmark: Boolean,
    onRemoveBookmark: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.m)) {
        OutlinedButton(
            onClick = onRemoveBookmark,
            enabled = !isRemovingBookmark,
            modifier = Modifier.weight(1f),
            colors = BulletinButtonDefaults.outlinedButtonColors(),
            border = BulletinButtonDefaults.outlinedButtonBorder()
        ) {
            if (isRemovingBookmark) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = "Remove bookmark",
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isRemovingBookmark) "Removing..." else "Remove Bookmark")
        }
        Button(
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
            colors = BulletinButtonDefaults.buttonColors()
        ) {
            Text("Close")
        }
    }
}

@Composable
private fun ListingBadge(text: String, isPrimary: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (isPrimary) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (isPrimary) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = BulletinSpacing.xs)
        )
    }
}

@Composable
private fun BookmarkedListingSeller(sellerName: String, reputationScore: Double?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.m)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Person, "Seller Avatar", modifier = Modifier.size(24.dp))
                }
                Column {
                    Text(sellerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Verified CSULB Student",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.xs)
            ) {
                Icon(
                    Icons.Filled.Star,
                    "Reputation Score",
                    tint = BulletinExtras.colors.star,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    formatBookmarkedListingReputation(reputationScore),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

internal fun formatBookmarkedListingReputation(reputationScore: Double?): String {
    return if (reputationScore != null) {
        "${(reputationScore * 10).toInt() / 10.0} / 5.0"
    } else {
        "5.0 (New)"
    }
}
