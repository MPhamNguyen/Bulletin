package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
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

@Composable
internal fun BookmarkedListingPhotos(photos: List<String>) {
    if (photos.isEmpty()) {
        ListingPhotoPlaceholder("No photos uploaded for this listing", Modifier.fillMaxWidth())
    } else {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            items(photos) { photoUrl ->
                SubcomposeAsyncImage(
                    model = photoUrl,
                    contentDescription = "Listing photo",
                    modifier = Modifier.width(240.dp).height(180.dp).clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop,
                    error = { ListingPhotoPlaceholder("Photo unavailable", Modifier.width(240.dp)) },
                    success = { SubcomposeAsyncImageContent() }
                )
            }
        }
    }
}

@Composable
private fun ListingPhotoPlaceholder(label: String, modifier: Modifier) {
    Box(
        modifier = modifier
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}
