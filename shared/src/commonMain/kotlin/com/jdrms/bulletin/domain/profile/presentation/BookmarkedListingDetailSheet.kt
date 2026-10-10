package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Image
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
import com.jdrms.bulletin.core.designsystem.BulletinButtonDefaults
import com.jdrms.bulletin.core.designsystem.BulletinExtras
import com.jdrms.bulletin.core.designsystem.MessageSellerQuickAction
import com.jdrms.bulletin.core.designsystem.MessageSellerQuickActionActions
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListing

internal data class BookmarkedListingDetailActions(
    val onDismiss: () -> Unit,
    val onRemoveBookmark: () -> Unit,
    val canMessageSeller: Boolean,
    val onMessageSeller: suspend (String) -> Boolean,
    val hasExistingConversation: suspend () -> Boolean,
    val onMessageSent: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BookmarkedListingPhotos(listing.photos)
            ListingTitleAndPrice(listing)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            errorMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            ListingDetailActions(isRemovingBookmark, actions.onRemoveBookmark, actions.onDismiss)
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
        Spacer(modifier = Modifier.width(12.dp))
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
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun BookmarkedListingPhotos(photos: List<String>) {
    if (photos.isEmpty()) {
        ListingPhotoPlaceholder("No photos uploaded for this listing", Modifier.fillMaxWidth())
    } else {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            items(photos) {
                ListingPhotoPlaceholder("Photo Preview", Modifier.width(240.dp))
            }
        }
    }
}

@Composable
private fun ListingPhotoPlaceholder(label: String, modifier: Modifier) {
    Box(
        modifier = modifier
            .height(180.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                horizontalArrangement = Arrangement.spacedBy(4.dp)
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
