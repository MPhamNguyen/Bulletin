package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.core.common.formatRelativeTime
import com.jdrms.bulletin.core.designsystem.theme.BulletinElevation
import com.jdrms.bulletin.core.designsystem.theme.BulletinShapes
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem

/** Compact image-first cards with consistent single-line information rows. */
@Composable
internal fun MarketplaceItemCard(
    item: MarketplaceItem,
    isBookmarked: Boolean,
    onBookmarkClick: () -> Unit,
    onCardClick: () -> Unit,
    onOriginCaptured: (ListingZoomOrigin?) -> Unit = {}
) {
    val bounds = remember { ListingBoundsTracker() }
    Card(
        modifier = Modifier.fillMaxWidth().wrapContentHeight()
            .onGloballyPositioned(bounds::trackCard)
            .clickable {
                onOriginCaptured(bounds.capture(item))
                onCardClick()
            },
        shape = BulletinShapes.listingCard,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = BulletinElevation.none)
    ) {
        Column {
            MarketplaceListingPhoto(
                item,
                isBookmarked,
                onBookmarkClick,
                Modifier.fillMaxWidth().onGloballyPositioned(bounds::trackPhoto)
            )
            ListingDetails(item)
        }
    }
}

@Composable
private fun ListingDetails(item: MarketplaceItem) {
    Column(
        modifier = Modifier.padding(BulletinSpacing.m),
        verticalArrangement = Arrangement.spacedBy(BulletinSpacing.xs)
    ) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = item.price.formatted,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
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
