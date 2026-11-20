package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing

@Composable
internal fun MarketplaceListingDescription(listing: Listing) {
    MarketplaceListingDescription(listing.description, listing.createdAtMillis, listing.updatedAtMillis)
}

@Composable
internal fun MarketplaceListingDescription(description: String, createdAtMillis: Long, updatedAtMillis: Long) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "About this item",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatMarketplaceListingTime(
                createdAtMillis = createdAtMillis,
                updatedAtMillis = updatedAtMillis,
                nowMillis = currentTimeMillis()
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
