package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem

@Composable
internal fun MarketplaceInterestMatches(
    listings: List<MarketplaceItem>,
    isLoading: Boolean,
    onListingClick: (String) -> Unit
) {
    Text("Matching listings", style = MaterialTheme.typography.titleMedium)
    when {
        isLoading && listings.isEmpty() -> Text(
            "Finding listings…",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        listings.isEmpty() -> Text(
            "New listings matching your interests will appear here.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        else -> listings.forEach { listing ->
            BulletinCard(
                modifier = Modifier.clickable { onListingClick(listing.id.value) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.m),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = listing.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = listing.price.formatted,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Text(
                    text = "${listing.category.displayName()} · ${listing.sellerName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
