package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceListingStatus

@Composable
internal fun ListingSellerReviewAction(state: MarketplaceDetailSheetState, onLeaveReview: () -> Unit) {
    if (state.canLeaveSellerReview()) {
        BulletinButton(
            onClick = onLeaveReview,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading
        ) {
            Text("Leave Review for Seller")
        }
    }
}

private fun MarketplaceDetailSheetState.canLeaveSellerReview(): Boolean =
    listing?.status == MarketplaceListingStatus.SOLD && listing.buyerId == currentUserId && !hasReview
