package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.jdrms.bulletin.core.common.cropSettings
import com.jdrms.bulletin.core.common.imageRequestUrl

@Composable
internal fun ConversationListingPreview(imageUrl: String?, imageSize: Dp = 64.dp) {
    if (imageUrl == null) {
        ListingPreviewPlaceholder(imageSize)
        return
    }
    val crop = imageUrl.cropSettings()
    SubcomposeAsyncImage(
        model = imageUrl.imageRequestUrl(),
        contentDescription = "Listing image",
        modifier = Modifier.size(imageSize).clip(RoundedCornerShape(10.dp)).clipToBounds(),
        contentScale = ContentScale.Crop,
        loading = { ListingPreviewPlaceholder(imageSize) },
        error = { ListingPreviewPlaceholder(imageSize) },
        success = {
            SubcomposeAsyncImageContent(
                Modifier.graphicsLayer {
                    scaleX = crop.scale
                    scaleY = crop.scale
                    translationX = -crop.offsetX * this.size.width * crop.scale
                    translationY = -crop.offsetY * this.size.height * crop.scale
                }
            )
        }
    )
}

@Composable
private fun ListingPreviewPlaceholder(size: Dp) {
    androidx.compose.foundation.layout.Box(
        Modifier.size(size),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Icon(
            Icons.Outlined.Image,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
