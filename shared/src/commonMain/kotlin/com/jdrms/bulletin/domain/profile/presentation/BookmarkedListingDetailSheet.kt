package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.core.common.formatRelativeTime
import com.jdrms.bulletin.core.designsystem.BulletinButtonDefaults
import com.jdrms.bulletin.core.designsystem.ItemDetailRow
import com.jdrms.bulletin.core.designsystem.ItemDetailsSection
import com.jdrms.bulletin.core.designsystem.MessageSellerQuickAction
import com.jdrms.bulletin.core.designsystem.MessageSellerQuickActionActions
import com.jdrms.bulletin.core.designsystem.ReviewSubmissionDialog
import com.jdrms.bulletin.core.designsystem.dismissKeyboardOnTapOutside
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListing

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
            modifier = Modifier.dismissKeyboardOnTapOutside()
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
                Text(
                    text = formatRelativeTime(listing.createdAtMillis, currentTimeMillis()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ItemDetailsSection(listing.attributes.map { ItemDetailRow(it.label, it.value) })
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
