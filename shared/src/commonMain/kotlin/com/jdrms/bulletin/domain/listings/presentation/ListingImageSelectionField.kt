package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.jdrms.bulletin.core.common.ImageCropSettings
import com.jdrms.bulletin.core.common.cropSettings
import com.jdrms.bulletin.core.common.imageRequestUrl
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.BulletinSpacing
import com.jdrms.bulletin.domain.listings.domain.model.ListingImageUpload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun ListingImageSelectionField(
    photos: List<ListingPhotoDraft>,
    onPick: () -> Unit,
    onRemove: (Int) -> Unit,
    onCropChange: (Int, ImageCropSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    BulletinCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Listing photos", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Up to ${ListingImageUpload.MAX_LISTING_IMAGE_COUNT} images, 5 MB each. " +
                        "Adjust each photo to fit the square.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onPick, enabled = photos.size < ListingImageUpload.MAX_LISTING_IMAGE_COUNT) {
                Text("Add photos")
            }
        }
        if (photos.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s)) {
                itemsIndexed(photos) { index, photo ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val crop = when (photo) {
                            is ListingPhotoDraft.Existing -> photo.url.cropSettings()
                            is ListingPhotoDraft.Pending -> ImageCropSettings(
                                photo.upload.cropScale,
                                photo.upload.cropOffsetX,
                                photo.upload.cropOffsetY
                            )
                        }
                        ListingPhotoImage(
                            photo = photo,
                            crop = crop,
                            description = "Listing photo ${index + 1}",
                            modifier = Modifier.size(92.dp).clip(MaterialTheme.shapes.small)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { editingIndex = index }) { Text("Adjust") }
                            IconButton(onClick = { onRemove(index) }) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove photo")
                            }
                        }
                    }
                }
            }
        }
    }
    editingIndex?.let { index ->
        photos.getOrNull(index)?.let { photo ->
            val crop = when (photo) {
                is ListingPhotoDraft.Existing -> photo.url.cropSettings()
                is ListingPhotoDraft.Pending -> ImageCropSettings(
                    photo.upload.cropScale,
                    photo.upload.cropOffsetX,
                    photo.upload.cropOffsetY
                )
            }
            ListingPhotoCropDialog(
                photo = photo,
                initialCrop = crop,
                onDismiss = { editingIndex = null },
                onSave = { updatedCrop ->
                    onCropChange(index, updatedCrop)
                    editingIndex = null
                }
            )
        }
    }
}

private val ImageCropSettings.maxOffset: Float
    get() = ((scale - 1f) / (2f * scale)).coerceAtLeast(0f)

@Composable
private fun ListingPhotoImage(
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
private fun ListingPhotoCropDialog(
    photo: ListingPhotoDraft,
    initialCrop: ImageCropSettings,
    onDismiss: () -> Unit,
    onSave: (ImageCropSettings) -> Unit
) {
    val cropState = remember(photo) { mutableStateOf(initialCrop) }
    val crop by cropState
    var frameSize by remember { mutableStateOf(1f) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.widthIn(max = 360.dp).clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surface).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(BulletinSpacing.m)
        ) {
            Text("Adjust photo", style = MaterialTheme.typography.titleMedium)
            ListingPhotoImage(
                photo = photo,
                crop = crop,
                description = "Photo crop preview",
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.medium)
                    .onSizeChanged { frameSize = it.width.toFloat().coerceAtLeast(1f) }
                    .pointerInput(photo, frameSize) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val current = cropState.value
                            val scale = (current.scale * zoom).coerceIn(1f, 4f)
                            val maxOffset = ((scale - 1f) / (2f * scale)).coerceAtLeast(0f)
                            cropState.value = ImageCropSettings(
                                scale = scale,
                                offsetX = (current.offsetX - pan.x / frameSize / current.scale)
                                    .coerceIn(-maxOffset, maxOffset),
                                offsetY = (current.offsetY - pan.y / frameSize / current.scale)
                                    .coerceIn(-maxOffset, maxOffset)
                            )
                        }
                    }
            )
            Text(
                "Pinch to zoom and drag to reposition",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(onClick = { onSave(crop) }, modifier = Modifier.weight(1f)) { Text("Save crop") }
            }
        }
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
