package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
        else -> Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(BulletinSpacing.m)
        ) {
            listings.forEach { listing ->
                MarketplaceItemCard(
                    item = listing,
                    columns = 1,
                    isBookmarked = false,
                    onBookmarkClick = { /* no-op in interests view */ },
                    onCardClick = { onListingClick(listing.id.value) }
                )
            }
        }
    }
}
