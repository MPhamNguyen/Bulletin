package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.jdrms.bulletin.core.common.cropSettings
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.core.common.formatRelativeTime
import com.jdrms.bulletin.core.common.imageRequestUrl
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinElevation
import com.jdrms.bulletin.core.designsystem.theme.BulletinShapes
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListing

@Composable
internal fun BookmarkedListingCard(
    listing: ProfileBookmarkedListing,
    isRemoving: Boolean,
    bookmarkActionEnabled: Boolean,
    onClick: () -> Unit,
    onRemoveBookmark: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable(onClick = onClick),
        shape = BulletinShapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = BulletinElevation.low)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(BOOKMARKED_CARD_PADDING),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BookmarkedListingPhoto(listing = listing)
            Column(modifier = Modifier.weight(1f).padding(start = BulletinSpacing.m)) {
                val listingTime = formatRelativeTime(listing.createdAtMillis, currentTimeMillis())
                val conditionLabel = listing.condition
                    .replace('_', ' ')
                    .lowercase()
                    .replaceFirstChar(Char::uppercase)
                BookmarkedListingTitleAndPrice(listing)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Condition: $conditionLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$listingTime • ${listing.sellerName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(BulletinSpacing.s))
                BookmarkedListingButton(
                    isRemoving = isRemoving,
                    enabled = bookmarkActionEnabled,
                    onClick = onRemoveBookmark
                )
            }
        }
    }
}

@Composable
private fun BookmarkedListingPhoto(listing: ProfileBookmarkedListing) {
    val photoUrl = listing.photos.firstOrNull()
    Box(
        modifier = Modifier
            .size(BOOKMARKED_PHOTO_SIZE)
            .clip(MaterialTheme.shapes.medium)
    ) {
        if (photoUrl == null) {
            BookmarkedPhotoPlaceholder()
        } else {
            SubcomposeAsyncImage(
                model = photoUrl.imageRequestUrl(),
                contentDescription = "${listing.title} photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = { BookmarkedPhotoPlaceholder() },
                error = { BookmarkedPhotoPlaceholder() },
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
    }
}

@Composable
private fun BookmarkedListingButton(
    isRemoving: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BulletinButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        variant = BulletinButtonVariant.Neutral,
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        if (isRemoving) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onSurface
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Bookmark,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (isRemoving) "Removing..." else "Remove Bookmark",
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun BookmarkedPhotoPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Image,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(56.dp)
        )
    }
}

@Composable
private fun BookmarkedListingTitleAndPrice(listing: ProfileBookmarkedListing) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = listing.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = listing.price,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private val BOOKMARKED_CARD_PADDING = 12.dp
private val BOOKMARKED_PHOTO_SIZE = 112.dp
