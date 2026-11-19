package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.core.designsystem.BulletinSkeletonBlock
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem

/** Read-only card snapshot while availability, seller details and review eligibility are checked. */
@Composable
internal fun ColumnScope.MarketplaceDetailPreview(item: MarketplaceItem, isBookmarked: Boolean) {
    ListingPhotosSection(photos = item.photos)
    Column(verticalArrangement = Arrangement.spacedBy(BulletinSpacing.xs)) {
        Text(item.title, style = MaterialTheme.typography.titleLarge)
        Text(item.price.formatted, style = MaterialTheme.typography.headlineSmall)
        Text(
            formatMarketplaceListingTime(item.createdAtMillis, item.updatedAtMillis, currentTimeMillis()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isBookmarked) Text("Saved", style = MaterialTheme.typography.labelMedium)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    SellerInfoCardSkeleton(name = item.sellerName)
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    if (item.description.isNotBlank()) {
        Text("Description", style = MaterialTheme.typography.titleMedium)
        Text(item.description, style = MaterialTheme.typography.bodyMedium)
    } else {
        BulletinSkeletonBlock(Modifier.fillMaxWidth().height(48.dp))
    }
    Text(
        "Checking listing availability…",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
