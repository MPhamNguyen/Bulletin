package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.jdrms.bulletin.core.common.cropSettings
import com.jdrms.bulletin.core.common.imageRequestUrl
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem

/** Square listing photo with the bookmark control pinned to its top-right corner. */
@Composable
internal fun MarketplaceListingPhoto(
    item: MarketplaceItem,
    isBookmarked: Boolean,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val photoUrl = item.photos.firstOrNull()
    Box(modifier = modifier.aspectRatio(1f).clipToBounds()) {
        if (photoUrl == null) {
            MarketplaceListingPhotoFallback()
        } else {
            SubcomposeAsyncImage(
                model = photoUrl.imageRequestUrl(),
                contentDescription = "${item.title} photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = { MarketplaceListingPhotoFallback() },
                error = { MarketplaceListingPhotoFallback() },
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
        MarketplaceBookmarkButton(
            isBookmarked = isBookmarked,
            onClick = onBookmarkClick,
            modifier = Modifier.align(Alignment.TopEnd).padding(BOOKMARK_CORNER_INSET)
        )
    }
}

@Composable
private fun MarketplaceBookmarkButton(
    isBookmarked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier.size(48.dp)) {
        Box(
            modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.background, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark listing",
                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun MarketplaceListingPhotoFallback() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Image,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(34.dp)
        )
    }
}

// The 48dp touch target is centered on a 36dp circle; this inset puts the circle 10dp from the corner.
private val BOOKMARK_CORNER_INSET = 4.dp
