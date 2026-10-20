package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.core.common.formatRelativeTime
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem

@Composable
internal fun MarketplaceItemCard(
    item: MarketplaceItem,
    columns: Int = 1,
    onCardClick: () -> Unit
) {
    if (columns == 1) {
        SingleColumnListingCard(item, onCardClick)
    } else {
        TwoColumnListingCard(item, onCardClick)
    }
}

@Composable
private fun SingleColumnListingCard(item: MarketplaceItem, onCardClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(SINGLE_CARD_HEIGHT)
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            MarketplaceListingPhoto(item, SINGLE_PHOTO_HEIGHT)
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                SingleListingTitleAndPrice(item)
                Spacer(modifier = Modifier.height(2.dp))
                ListingMetadata(item, MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun TwoColumnListingCard(item: MarketplaceItem, onCardClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(DOUBLE_CARD_HEIGHT)
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            MarketplaceListingPhoto(item, DOUBLE_PHOTO_HEIGHT)
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.height(40.dp)
                )
                Text(
                    text = item.price.formatted,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                ListingMetadata(item, MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun MarketplaceListingPhoto(item: MarketplaceItem, height: androidx.compose.ui.unit.Dp) {
    val photoUrl = item.photos.firstOrNull()
    if (photoUrl == null) {
        MarketplaceListingPhotoFallback(height)
        return
    }
    SubcomposeAsyncImage(
        model = photoUrl,
        contentDescription = "${item.title} photo",
        modifier = Modifier.fillMaxWidth().height(height),
        contentScale = ContentScale.Crop,
        loading = { MarketplaceListingPhotoFallback(height) },
        error = { MarketplaceListingPhotoFallback(height) },
        success = { SubcomposeAsyncImageContent() }
    )
}

@Composable
private fun MarketplaceListingPhotoFallback(height: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
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
    Text(
        text = "${formatRelativeTime(item.createdAtMillis, currentTimeMillis())} • ${item.sellerName}",
        style = style,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

private val SINGLE_PHOTO_HEIGHT = 196.dp
private val DOUBLE_PHOTO_HEIGHT = 140.dp
private val SINGLE_CARD_HEIGHT = 264.dp
private val DOUBLE_CARD_HEIGHT = 240.dp
