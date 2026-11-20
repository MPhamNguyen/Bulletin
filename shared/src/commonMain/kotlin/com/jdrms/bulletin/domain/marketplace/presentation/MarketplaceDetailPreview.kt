package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem

/** Uses the final listing sections for known data while unknown seller and action data loads. */
@Composable
internal fun ColumnScope.MarketplaceDetailPreview(item: MarketplaceItem) {
    ListingPhotosSection(photos = item.photos)
    MarketplaceListingHeader(item.title, item.price.formatted, item.category.name, item.condition.name)
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    SellerInfoCard(seller = null, previewName = item.sellerName, isLoading = true)
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    MarketplaceListingDescription(item.description, item.createdAtMillis, item.updatedAtMillis)
    MarketplaceItemDetails(item.category, item.attributes)
}
