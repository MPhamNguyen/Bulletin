package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.domain.listings.domain.model.ListingImageUpload

@Composable
internal fun ListingImageSelectionField(
    photos: List<ListingPhotoDraft>,
    onPick: () -> Unit,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BulletinCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Listing photos", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Up to ${ListingImageUpload.MAX_LISTING_IMAGE_COUNT} images, 5 MB each",
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
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(photos) { index, photo ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        SubcomposeAsyncImage(
                            model = when (photo) {
                                is ListingPhotoDraft.Existing -> photo.url
                                is ListingPhotoDraft.Pending -> photo.upload.bytes
                            },
                            contentDescription = "Listing photo ${index + 1}",
                            modifier = Modifier.size(92.dp).clip(MaterialTheme.shapes.small),
                            contentScale = ContentScale.Crop,
                            loading = { CircularProgressIndicator() },
                            error = { PhotoPlaceholder() },
                            success = { SubcomposeAsyncImageContent() }
                        )
                        IconButton(onClick = { onRemove(index) }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove photo")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoPlaceholder() {
    Box(
        modifier = Modifier.size(92.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Outlined.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
