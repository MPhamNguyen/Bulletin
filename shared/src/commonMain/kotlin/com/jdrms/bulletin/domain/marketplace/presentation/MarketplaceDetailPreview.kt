package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem

/** Uses the final listing sections for known data while unknown seller and action data loads. */
@Composable
internal fun ColumnScope.MarketplaceDetailPreview(item: MarketplaceItem, currentUserId: String?) {
    ListingPhotosSection(photos = item.photos)
    MarketplaceListingHeader(item.title, item.price.formatted, item.category.name, item.condition.name)
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    SellerInfoCardSkeleton(name = item.sellerName)
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    MarketplaceListingDescription(item.description, item.createdAtMillis, item.updatedAtMillis)
    MarketplaceItemDetails(item.category, item.attributes)
    ListingActionsSkeleton(showBuyerActions = currentUserId != item.sellerId)
}
