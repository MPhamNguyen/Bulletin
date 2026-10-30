package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.common.ImageCropSettings
import com.jdrms.bulletin.core.common.cropSettings
import com.jdrms.bulletin.domain.listings.domain.model.ListingImageUpload

@Composable
internal fun ListingImageSelectionField(
    photos: List<ListingPhotoDraft>,
    onPick: () -> Unit,
    onRemove: (Int) -> Unit,
    onCropChange: (Int, ImageCropSettings) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Photos",
    hint: String = "Up to ${ListingImageUpload.MAX_LISTING_IMAGE_COUNT} photos, 5 MB each. The first is the cover."
) {
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    Column(modifier = modifier.fillMaxWidth()) {
        if (label.isNotBlank()) FieldLabel(label)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        modifier = Modifier.size(ListingPhotoTileSize).clip(MaterialTheme.shapes.small)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { editingIndex = index }) { Text("Adjust") }
                        IconButton(onClick = { onRemove(index) }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove photo")
                        }
                    }
                }
            }
            if (photos.size < ListingImageUpload.MAX_LISTING_IMAGE_COUNT) {
                item { ListingPhotoAddTile(onClick = onPick) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
