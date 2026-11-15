package com.jdrms.bulletin.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

@Composable
internal fun ReviewRequestPurchasePreview(
    title: String,
    sellerName: String,
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "$title listing photo",
                modifier = Modifier.fillMaxWidth().weight(1f).clip(MaterialTheme.shapes.large),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) { }
        }
        Text(title, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium)
        Text(
            "Seller: $sellerName",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun ReviewRequestListingPreview(title: String, imageUrl: String?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.m),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "$title listing photo",
                modifier = Modifier.size(width = 92.dp, height = 72.dp).clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(
                modifier = Modifier.size(width = 92.dp, height = 72.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) { }
        }
        Column(verticalArrangement = Arrangement.spacedBy(BulletinSpacing.xs)) {
            Text("Listing", style = MaterialTheme.typography.labelMedium)
            Text(title, style = MaterialTheme.typography.titleSmall)
        }
    }
}
