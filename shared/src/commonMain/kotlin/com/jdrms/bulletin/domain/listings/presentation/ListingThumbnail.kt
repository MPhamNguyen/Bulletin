package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.jdrms.bulletin.core.common.cropSettings
import com.jdrms.bulletin.core.common.imageRequestUrl
import com.jdrms.bulletin.domain.listings.domain.model.Listing

@Composable
internal fun ListingThumbnail(listing: Listing, modifier: Modifier = Modifier) {
    val photoUrl = listing.images.firstOrNull()
    val thumbnailModifier = modifier.size(88.dp).clip(MaterialTheme.shapes.small)
    if (photoUrl == null) {
        ListingThumbnailPlaceholder(thumbnailModifier)
        return
    }
    SubcomposeAsyncImage(
        model = photoUrl.imageRequestUrl(),
        contentDescription = "${listing.title} thumbnail",
        modifier = thumbnailModifier,
        contentScale = ContentScale.Crop,
        loading = { ListingThumbnailPlaceholder(Modifier.fillMaxSize()) },
        error = { ListingThumbnailPlaceholder(Modifier.fillMaxSize()) },
        success = {
            val crop = photoUrl.cropSettings()
            SubcomposeAsyncImageContent(
                Modifier.graphicsLayer {
                    scaleX = crop.scale
                    scaleY = crop.scale
                    translationX = -crop.offsetX * size.width * crop.scale
                    translationY = -crop.offsetY * size.height * crop.scale
                }
            )
        }
    )
}

@Composable
private fun ListingThumbnailPlaceholder(modifier: Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Image,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(32.dp)
        )
    }
}
