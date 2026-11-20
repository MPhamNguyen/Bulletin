package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.jdrms.bulletin.core.designsystem.ItemDetailsSection

/** Unknown fields use the same section layouts as loaded and preview content. */
@Composable
internal fun ColumnScope.ListingDetailSkeleton() {
    ListingPhotosSection(emptyList(), isLoading = true)
    MarketplaceListingHeader(null, null, null, null, isLoading = true)
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    SellerInfoCard(seller = null, isLoading = true)
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    MarketplaceListingDescription(null, null, null, isLoading = true)
    ItemDetailsSection(emptyList(), isLoading = true)
    ListingActionsSkeleton()
}
