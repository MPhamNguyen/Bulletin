package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.jdrms.bulletin.core.common.ImageCropSettings
import com.jdrms.bulletin.core.common.imageRequestUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun ListingPhotoImage(
    photo: ListingPhotoDraft,
    crop: ImageCropSettings,
    description: String,
    modifier: Modifier = Modifier
) {
    when (photo) {
        is ListingPhotoDraft.Existing -> ExistingListingPhotoImage(photo, crop, description, modifier)
        is ListingPhotoDraft.Pending -> PendingListingPhotoImage(photo, crop, description, modifier)
    }
}

@Composable
private fun ExistingListingPhotoImage(
    photo: ListingPhotoDraft.Existing,
    crop: ImageCropSettings,
    description: String,
    modifier: Modifier
) {
    SubcomposeAsyncImage(
        model = photo.url.imageRequestUrl(),
        contentDescription = description,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        loading = { CircularProgressIndicator() },
        error = { PhotoPlaceholder() },
        success = {
            SubcomposeAsyncImageContent(
                modifier = Modifier.graphicsLayer {
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
private fun PendingListingPhotoImage(
    photo: ListingPhotoDraft.Pending,
    crop: ImageCropSettings,
    description: String,
    modifier: Modifier
) {
    val bitmap by produceState<ImageBitmap?>(null, photo.upload) {
        value = withContext(Dispatchers.Default) { decodeListingImagePreview(photo.upload.bytes) }
    }
    val preview = bitmap
    if (preview == null) {
        Box(modifier = modifier) { PhotoPlaceholder() }
    } else {
        androidx.compose.foundation.Image(
            bitmap = preview,
            contentDescription = description,
            modifier = modifier.graphicsLayer {
                scaleX = crop.scale
                scaleY = crop.scale
                translationX = -crop.offsetX * size.width * crop.scale
                translationY = -crop.offsetY * size.height * crop.scale
            },
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun PhotoPlaceholder() {
    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Outlined.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
