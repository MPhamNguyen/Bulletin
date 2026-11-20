package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.core.designsystem.MessageSellerQuickActionContent
import com.jdrms.bulletin.core.designsystem.MessageSellerQuickActionStatus
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

/** Uses disabled final controls so typography, padding and height have one owner. */
@Composable
internal fun ColumnScope.ListingActionsSkeleton(showBuyerActions: Boolean = true) {
    Spacer(Modifier.height(BulletinSpacing.xs))
    if (showBuyerActions) {
        MessageSellerQuickActionContent(
            status = MessageSellerQuickActionStatus("Hi, is this available?", "Send", false, false),
            onValueChange = {},
            onSend = {},
            isLoading = true
        )
    }
    ListingDetailActions(
        isBookmarked = false,
        bookmarkErrorMessage = null,
        callbacks = ListingDetailActionCallbacks({}, {}, {}),
        canBookmarkListing = showBuyerActions,
        canReportListing = showBuyerActions,
        isLoading = true
    )
}
