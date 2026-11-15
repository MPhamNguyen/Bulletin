package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.core.common.formatRelativeTime
import com.jdrms.bulletin.core.designsystem.theme.BulletinElevation
import com.jdrms.bulletin.core.designsystem.theme.BulletinShapes
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem

/** Image-first cards with a description preview in the single-column layout. */
@Composable
internal fun MarketplaceItemCard(
    item: MarketplaceItem,
    columns: Int = 1,
    isBookmarked: Boolean,
    onBookmarkClick: () -> Unit,
    onCardClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().wrapContentHeight().clickable(onClick = onCardClick),
        shape = BulletinShapes.listingCard,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        elevation = CardDefaults.cardElevation(defaultElevation = BulletinElevation.none)
    ) {
        Column {
            MarketplaceListingPhoto(item, isBookmarked, onBookmarkClick, Modifier.fillMaxWidth())
            ListingDetails(item, showDescription = columns == 1)
        }
    }
}

@Composable
private fun ListingDetails(item: MarketplaceItem, showDescription: Boolean) {
    Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = item.price.formatted,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        if (showDescription && item.description.isNotBlank()) {
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        ListingMetadata(item)
    }
}

@Composable
private fun ListingMetadata(item: MarketplaceItem) {
    val listingTime = formatMarketplaceListingTime(
        item.createdAtMillis,
        item.updatedAtMillis,
        currentTimeMillis()
    )
    Text(
        text = "$listingTime · ${item.sellerName}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

internal fun formatMarketplaceListingTime(
    createdAtMillis: Long,
    updatedAtMillis: Long,
    nowMillis: Long
): String = if (updatedAtMillis > createdAtMillis && updatedAtMillis > 0L) {
    "Edited ${formatRelativeTime(updatedAtMillis, nowMillis)}"
} else {
    formatRelativeTime(createdAtMillis, nowMillis)
}
