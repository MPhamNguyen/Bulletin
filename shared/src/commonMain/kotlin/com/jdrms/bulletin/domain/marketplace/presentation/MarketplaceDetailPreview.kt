package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.core.designsystem.ItemDetailsSection

/** Uses the final listing sections for known data while unknown seller and action data loads. */
@Composable
internal fun ColumnScope.MarketplaceDetailPreview(
    item: MarketplaceListingPreview,
    photoModifier: Modifier = Modifier
) {
    val content = item.content
    Box(photoModifier) { ListingPhotosSection(photos = content.photos) }
    MarketplaceListingHeader(content.title, content.price, content.category, content.condition)
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    SellerInfoCard(seller = null, previewName = item.sellerName, isLoading = true)
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    MarketplaceListingDescription(content.description, content.createdAtMillis, content.updatedAtMillis)
    ItemDetailsSection(content.attributes)
}
