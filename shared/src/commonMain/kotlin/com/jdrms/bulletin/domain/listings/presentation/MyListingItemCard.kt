package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.ModeEdit
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingStatus

@Composable
fun MyListingItemCard(
    listing: Listing,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMarkSold: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    BulletinCard(modifier = modifier) {
        MyListingSummary(listing)
        ListingAttributeSummary(listing, Modifier.padding(top = 6.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = listing.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        MyListingSoldBadge(listing.status)
        MyListingImageCount(listing.images.size)
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(8.dp))
        MyListingStatusButton(listing.status, onMarkSold, onRestore)
        MyListingManagementActions(onDelete, onEdit)
    }
}

@Composable
private fun MyListingSummary(listing: Listing) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        ListingThumbnail(listing)
        Column(modifier = Modifier.weight(1f)) {
            MyListingTitleAndPrice(listing)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MyListingMetadataChip(listing.category.name)
                MyListingMetadataChip(listing.condition.name.replace("_", " "))
            }
        }
    }
}

@Composable
private fun MyListingTitleAndPrice(listing: Listing) {
    Text(
        text = listing.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = listing.price.formatted,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun MyListingMetadataChip(label: String) {
    Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun MyListingSoldBadge(status: ListingStatus) {
    if (status != ListingStatus.SOLD) return
    Spacer(modifier = Modifier.height(8.dp))
    Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(
            text = "SOLD",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun MyListingImageCount(imageCount: Int) {
    if (imageCount == 0) return
    Spacer(modifier = Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(
            imageVector = Icons.Outlined.Image,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "$imageCount image(s) attached",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun MyListingStatusButton(status: ListingStatus, onMarkSold: () -> Unit, onRestore: () -> Unit) {
    when (status) {
        ListingStatus.AVAILABLE -> MyListingStateButton("Mark as sold", onMarkSold)
        ListingStatus.SOLD -> MyListingStateButton("Restore to marketplace", onRestore)
        else -> Unit
    }
}

@Composable
private fun MyListingStateButton(label: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Icon(Icons.Outlined.Sell, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label)
    }
}

@Composable
private fun MyListingManagementActions(onDelete: () -> Unit, onEdit: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onDelete,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Delete")
        }
        FilledTonalButton(
            onClick = onEdit,
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Icon(Icons.Outlined.ModeEdit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Edit Listing", style = MaterialTheme.typography.labelLarge)
        }
    }
}
