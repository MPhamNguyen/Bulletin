package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.MessageSellerQuickAction
import com.jdrms.bulletin.core.designsystem.MessageSellerQuickActionActions
import com.jdrms.bulletin.core.designsystem.theme.BulletinMotion
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceListingStatus

/** Keeps action identity and message drafts stable as preview data becomes full details. */
@Composable
internal fun ListingInteractionSection(state: MarketplaceDetailSheetState, actions: MarketplaceDetailSheetActions) {
    val listing = state.listing
    val listingId = listing?.id?.value ?: state.preview?.id?.value
    val sellerId = listing?.sellerId ?: state.preview?.sellerId
    val hasListingIdentity = listingId != null && sellerId != null
    val sellerName = listing?.sellerName ?: state.preview?.sellerName.orEmpty()
    val showBuyerActions = sellerId == null || state.currentUserId != sellerId
    Column(
        modifier = Modifier.fillMaxWidth().animateContentSize(tween(BulletinMotion.STANDARD)),
        verticalArrangement = Arrangement.spacedBy(BulletinSpacing.l)
    ) {
        if (state.canLeaveSellerReview()) {
            BulletinButton(onClick = actions.onLeaveReview, modifier = Modifier.fillMaxWidth()) {
                Text("Leave Review for Seller")
            }
        }
        if (showBuyerActions) {
            MessageSellerQuickAction(
                canSend = hasListingIdentity,
                actions = MessageSellerQuickActionActions(
                    conversationKey = "$listingId:$sellerId",
                    hasExistingConversation = {
                        listingId != null && sellerId != null &&
                            actions.hasExistingConversation(listingId, sellerId)
                    },
                    onSend = { content ->
                        listingId != null && sellerId != null &&
                            actions.onMessageSeller(listingId, sellerId, sellerName, content)
                    },
                    onSent = {
                        actions.onDismiss()
                        actions.onMessageSent()
                    },
                    onSeeChat = {
                        if (listingId != null && sellerId != null) actions.onSeeChat(listingId, sellerId)
                    }
                )
            )
        }
        ListingDetailActions(
            isBookmarked = state.isBookmarked,
            bookmarkErrorMessage = state.bookmarkErrorMessage,
            callbacks = ListingDetailActionCallbacks(
                actions.onToggleBookmark,
                actions.onDismiss,
                actions.onReportListing
            ),
            canBookmarkListing = showBuyerActions,
            canReportListing = showBuyerActions,
            isLoading = listing == null,
            bookmarkEnabled = hasListingIdentity
        )
    }
}

private fun MarketplaceDetailSheetState.canLeaveSellerReview(): Boolean =
    listing?.status == MarketplaceListingStatus.SOLD && listing.buyerId == currentUserId && !hasReview
