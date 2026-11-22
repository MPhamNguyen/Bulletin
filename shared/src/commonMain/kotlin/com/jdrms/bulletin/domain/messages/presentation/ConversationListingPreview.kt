package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.jdrms.bulletin.core.common.cropSettings

@Composable
internal fun ConversationListingPreview(imageUrl: String?, imageSize: Dp = ConversationListingImageSize) {
    val context = LocalPlatformContext.current
    val sizePixels = with(LocalDensity.current) { imageSize.roundToPx() }
    Box(
        Modifier.size(imageSize).clip(MaterialTheme.shapes.small).clipToBounds()
    ) {
        ListingPreviewPlaceholder(imageSize)
        if (imageUrl.isNullOrBlank()) return@Box
        val crop = imageUrl.cropSettings()
        SubcomposeAsyncImage(
            model = conversationListingImageRequest(context, imageUrl, sizePixels),
            contentDescription = "Listing image",
            modifier = Modifier.size(imageSize),
            contentScale = ContentScale.Crop,
            loading = {},
            error = {},
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
}

@Composable
private fun ListingPreviewPlaceholder(size: Dp) {
    Box(
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
