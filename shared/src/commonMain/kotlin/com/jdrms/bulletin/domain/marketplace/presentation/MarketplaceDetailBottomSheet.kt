package com.jdrms.bulletin.domain.marketplace.presentation

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Warning
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
import com.jdrms.bulletin.core.designsystem.BulletinButtonDefaults
import com.jdrms.bulletin.core.designsystem.MessageSellerQuickAction
import com.jdrms.bulletin.core.designsystem.MessageSellerQuickActionActions
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing

data class MarketplaceDetailSheetState(
    val listing: Listing?,
    val isLoading: Boolean,
    val errorMessage: String?,
    val bookmarkErrorMessage: String?,
    val isBookmarked: Boolean,
    val currentUserId: String?
)

data class MarketplaceDetailSheetActions(
    val onDismiss: () -> Unit,
    val onRetry: () -> Unit,
    val onToggleBookmark: () -> Unit,
    val onSellerClick: (sellerId: String) -> Unit = {},
    val onMessageSeller: suspend (listingId: String, sellerId: String, sellerName: String, content: String) -> Boolean =
        { _, _, _, _ -> false },
    val hasExistingConversation: suspend (listingId: String, sellerId: String) -> Boolean =
        { _, _ -> false },
    val onSeeChat: suspend (listingId: String, sellerId: String) -> Unit = { _, _ -> }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceDetailBottomSheet(
    state: MarketplaceDetailSheetState,
    actions: MarketplaceDetailSheetActions
) {
    val listing = state.listing
    val isLoading = state.isLoading
    val errorMessage = state.errorMessage
    val bookmarkErrorMessage = state.bookmarkErrorMessage
    val isBookmarked = state.isBookmarked
    val onDismiss = actions.onDismiss
    val onRetry = actions.onRetry
    val onToggleBookmark = actions.onToggleBookmark
    val onSellerClick = actions.onSellerClick
    val onMessageSeller = actions.onMessageSeller
    val hasExistingConversation = actions.hasExistingConversation
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = "Loading listing details...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Warning,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = onRetry,
                                colors = BulletinButtonDefaults.buttonColors()
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }

                listing != null -> {
                    // Photos section
                    ListingPhotosSection(photos = listing.photos)

                    // Title & Price Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = listing.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = listing.price.formatted,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Metadata Badges (Category & Condition)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Text(
                                text = listing.category.name,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            val conditionText = listing.condition.replace("_", " ")
                            Text(
                                text = conditionText,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Seller Information & Reputation Score Card
                    SellerInfoCard(
                        seller = SellerInfoCardState(
                            name = listing.sellerName,
                            school = listing.sellerSchool,
                            avatarUrl = listing.sellerAvatarUrl,
                            reputationScore = listing.sellerReputationScore,
                            major = listing.sellerMajor,
                            reviewCount = listing.sellerReviewCount,
                            isVerified = listing.sellerIsVerified
                        ),
                        onClick = { onSellerClick(listing.sellerId) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Description Section
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "About this item",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = listing.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (state.currentUserId != listing.sellerId) {
                        MessageSellerQuickAction(
                            actions = MessageSellerQuickActionActions(
                                conversationKey = "${listing.id.value}:${listing.sellerId}",
                                hasExistingConversation = {
                                    hasExistingConversation(listing.id.value, listing.sellerId)
                                },
                                onSend = { content ->
                                    onMessageSeller(listing.id.value, listing.sellerId, listing.sellerName, content)
                                },
                                onSent = onDismiss,
                                onSeeChat = {
                                    actions.onSeeChat(listing.id.value, listing.sellerId)
                                }
                            )
                        )
                    }

                    // Action Buttons: Bookmark & Close
                    bookmarkErrorMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onToggleBookmark,
                            modifier = Modifier.weight(1f),
                            colors = BulletinButtonDefaults.outlinedButtonColors(),
                            border = BulletinButtonDefaults.outlinedButtonBorder()
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) {
                                    Icons.Filled.Bookmark
                                } else {
                                    Icons.Outlined.BookmarkBorder
                                },
                                contentDescription = if (isBookmarked) "Bookmarked" else "Bookmark",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isBookmarked) "Bookmarked" else "Bookmark")
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
            }
        }
    }
}
