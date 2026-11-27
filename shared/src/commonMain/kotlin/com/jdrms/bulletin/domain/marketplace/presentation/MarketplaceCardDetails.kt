package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem

/** The card's title, price and metadata rows. Title and price report their placement for the zoom transition. */
@Composable
internal fun MarketplaceCardDetails(item: MarketplaceItem, bounds: ListingBoundsTracker) {
    Column(
        modifier = Modifier.padding(BulletinSpacing.m),
        verticalArrangement = Arrangement.spacedBy(BulletinSpacing.xs)
    ) {
        val titleStyle = MaterialTheme.typography.titleSmall
        val priceStyle = MaterialTheme.typography.titleLarge
        val textColor = MaterialTheme.colorScheme.onSurface
        val title = bounds.probe(ListingTransitionElement.TITLE)
        val price = bounds.probe(ListingTransitionElement.PRICE)
        Text(
            text = item.title,
            style = titleStyle,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = title::laidOut,
            modifier = Modifier.listingTransitionSource(title, bounds.sourceVisibility, titleStyle, textColor)
        )
        Text(
            text = item.price.formatted,
            style = priceStyle,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = price::laidOut,
            modifier = Modifier.listingTransitionSource(price, bounds.sourceVisibility, priceStyle, textColor)
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        ListingMetadata(item)
    }
}

@Composable
private fun ListingMetadata(item: MarketplaceItem) {
    val listingTime = formatMarketplaceListingTime(
        item.createdAtMillis,
        item.editedAtMillis,
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
