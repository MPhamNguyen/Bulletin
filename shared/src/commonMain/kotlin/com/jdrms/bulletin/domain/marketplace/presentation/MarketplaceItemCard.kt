package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
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
    isBookmarked: Boolean,
    columns: Int = 1,
    onCardClick: () -> Unit,
    onToggleBookmark: () -> Unit
) {
    if (columns == 1) {
        FullWidthMarketplaceItemCard(item, isBookmarked, onCardClick, onToggleBookmark)
    } else {
        CompactMarketplaceItemCard(item, isBookmarked, onCardClick, onToggleBookmark)
    }
}

@Composable
private fun FullWidthMarketplaceItemCard(
    item: MarketplaceItem,
    isBookmarked: Boolean,
    onCardClick: () -> Unit,
    onToggleBookmark: () -> Unit
) {
    BulletinCard(modifier = Modifier.clickable(onClick = onCardClick)) {
        FullWidthMarketplaceItemHeader(item)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Seller: ${item.sellerName} • Category: ${item.category.name}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(item.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = formatRelativeTime(item.createdAtMillis, currentTimeMillis()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        FullWidthBookmarkButton(isBookmarked, onToggleBookmark)
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
            modifier = Modifier.weight(1f)
        )
        Text(
            text = item.price.formatted,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun FullWidthBookmarkButton(isBookmarked: Boolean, onToggleBookmark: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        OutlinedButton(onClick = onToggleBookmark) {
            Icon(
                imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = if (isBookmarked) "Bookmarked" else "Bookmark",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isBookmarked) "Bookmarked" else "Bookmark")
        }
    }
}

@Composable
private fun CompactMarketplaceItemCard(
    item: MarketplaceItem,
    isBookmarked: Boolean,
    onCardClick: () -> Unit,
    onToggleBookmark: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onCardClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            CompactMarketplaceItemDetails(item)
            Spacer(Modifier.height(6.dp))
            CompactBookmarkButton(isBookmarked, onToggleBookmark, Modifier.align(Alignment.End))
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

@Composable
private fun CompactBookmarkButton(
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onToggleBookmark, modifier = modifier.size(32.dp)) {
        Icon(
            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark"
        )
    }
}
