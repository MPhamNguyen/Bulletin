package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.core.common.cropSettings
import com.jdrms.bulletin.core.common.formatRelativeTime
import com.jdrms.bulletin.core.common.imageRequestUrl
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem

@Composable
internal fun MarketplaceItemCard(
    item: MarketplaceItem,
    columns: Int = 1,
    isBookmarked: Boolean,
    onBookmarkClick: () -> Unit,
    onCardClick: () -> Unit
) {
    if (columns == 1) {
        SingleColumnListingCard(item, isBookmarked, onBookmarkClick, onCardClick)
    } else {
        TwoColumnListingCard(item, isBookmarked, onBookmarkClick, onCardClick)
    }
}

@Composable
private fun SingleColumnListingCard(
    item: MarketplaceItem,
    isBookmarked: Boolean,
    onBookmarkClick: () -> Unit,
    onCardClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            MarketplaceListingPhoto(item, SINGLE_PHOTO_HEIGHT, isBookmarked, onBookmarkClick)
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                SingleListingTitleAndPrice(item)
                Spacer(modifier = Modifier.height(2.dp))
                ListingMetadata(item, MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun TwoColumnListingCard(
    item: MarketplaceItem,
    isBookmarked: Boolean,
    onBookmarkClick: () -> Unit,
    onCardClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            MarketplaceListingPhoto(item, DOUBLE_PHOTO_HEIGHT, isBookmarked, onBookmarkClick)
            TwoColumnListingDetails(item)
        }
    }
}

@Composable
private fun TwoColumnListingDetails(item: MarketplaceItem) {
    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = item.price.formatted,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        ListingMetadata(item, MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MarketplaceListingPhoto(
    item: MarketplaceItem,
    height: androidx.compose.ui.unit.Dp,
    isBookmarked: Boolean,
    onBookmarkClick: () -> Unit
) {
    val photoUrl = item.photos.firstOrNull()
    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clipToBounds()) {
        if (photoUrl == null) {
            MarketplaceListingPhotoFallback(height)
        } else {
            SubcomposeAsyncImage(
                model = photoUrl.imageRequestUrl(),
                contentDescription = "${item.title} photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                loading = { MarketplaceListingPhotoFallback(height) },
                error = { MarketplaceListingPhotoFallback(height) },
                success = {
                    val crop = photoUrl.cropSettings()
                    SubcomposeAsyncImageContent(Modifier.graphicsLayer {
                        scaleX = crop.scale
                        scaleY = crop.scale
                        translationX = -crop.offsetX * size.width * crop.scale
                        translationY = -crop.offsetY * size.height * crop.scale
                    })
                }
            )
        }
        MarketplaceBookmarkButton(
            isBookmarked = isBookmarked,
            compact = height == DOUBLE_PHOTO_HEIGHT,
            onClick = onBookmarkClick,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
        )
    }
}

@Composable
private fun MarketplaceBookmarkButton(
    isBookmarked: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonSize = if (compact) 34.dp else 40.dp
    val iconSize = if (compact) 18.dp else 22.dp
    IconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp)
    ) {
        Box(
            modifier = Modifier.size(buttonSize).background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shape = CircleShape
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark listing",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
private fun MarketplaceListingPhotoFallback(height: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Image,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(if (height == SINGLE_PHOTO_HEIGHT) 56.dp else 40.dp)
        )
    }
}

@Composable
private fun SingleListingTitleAndPrice(item: MarketplaceItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = item.price.formatted,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ListingMetadata(item: MarketplaceItem, style: androidx.compose.ui.text.TextStyle) {
    val listingTime = formatMarketplaceListingTime(
        item.createdAtMillis,
        item.updatedAtMillis,
        currentTimeMillis()
    )
    Text(
        text = "$listingTime • ${item.sellerName}",
        style = style,
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

private val SINGLE_PHOTO_HEIGHT = 216.dp
private val DOUBLE_PHOTO_HEIGHT = 180.dp
