package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.core.common.formatRelativeTime
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem

@Composable
internal fun MarketplaceItemCard(
    item: MarketplaceItem,
    columns: Int = 1,
    onCardClick: () -> Unit
) {
    if (columns == 1) {
        FullWidthMarketplaceItemCard(item, onCardClick)
    } else {
        CompactMarketplaceItemCard(item, onCardClick)
    }
}

@Composable
private fun FullWidthMarketplaceItemCard(
    item: MarketplaceItem,
    onCardClick: () -> Unit
) {
    BulletinCard(
        modifier = Modifier
            .height(FULL_WIDTH_CARD_HEIGHT)
            .clickable(onClick = onCardClick)
    ) {
        FullWidthMarketplaceItemHeader(item)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Seller: ${item.sellerName} • Category: ${item.category.name}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = formatRelativeTime(item.createdAtMillis, currentTimeMillis()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FullWidthMarketplaceItemHeader(item: MarketplaceItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = item.price.formatted,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun CompactMarketplaceItemCard(
    item: MarketplaceItem,
    onCardClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(COMPACT_CARD_HEIGHT)
            .clickable(onClick = onCardClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            CompactMarketplaceItemDetails(item)
        }
    }
}

@Composable
private fun CompactMarketplaceItemDetails(item: MarketplaceItem) {
    Text(
        text = item.title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = item.price.formatted,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    Text(
        text = item.sellerName,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

private val FULL_WIDTH_CARD_HEIGHT = 152.dp
private val COMPACT_CARD_HEIGHT = 112.dp
