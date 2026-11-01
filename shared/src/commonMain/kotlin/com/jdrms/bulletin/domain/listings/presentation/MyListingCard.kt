package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.domain.listings.domain.model.Listing

@Composable
internal fun MyListingCard(
    listing: Listing,
    onDelete: () -> Unit,
    onMarkSold: () -> Unit
) {
    BulletinCard {
        ListingThumbnail(listing)
        Spacer(modifier = Modifier.height(8.dp))
        ActiveListingTitleAndPrice(listing)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Category: ${listing.category.name} • Condition: ${listing.condition.name.replace("_", " ")}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ListingAttributeSummary(listing, Modifier.padding(top = 4.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = listing.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        ActiveListingActions(onDelete, onMarkSold)
    }
}

@Composable
private fun ActiveListingTitleAndPrice(listing: Listing) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = listing.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = listing.price.formatted,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ActiveListingActions(onDelete: () -> Unit, onMarkSold: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        FilledTonalButton(
            onClick = onMarkSold,
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) { Text("Mark as sold") }
        TextButton(
            onClick = onDelete,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Delete Listing")
        }
    }
}
