package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.SectionHeader
import com.jdrms.bulletin.domain.marketplace.domain.model.Category
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing

@Composable
fun MarketplaceScreen(viewModel: MarketplaceViewModel) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(
                    title = "Campus Marketplace",
                    subtitle = "Buy, sell, & trade with verified students"
                )
            }
        }

        item {
            Button(
                onClick = { viewModel.showCreateModal(true) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Create New Listing")
            }
        }

        item {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                label = { Text("Search listings...") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = state.selectedCategory == null,
                        onClick = { viewModel.onCategorySelected(null) },
                        label = { Text("All") }
                    )
                }
                items(Category.entries.toTypedArray()) { category ->
                    FilterChip(
                        selected = state.selectedCategory == category,
                        onClick = { viewModel.onCategorySelected(category) },
                        label = { Text(category.name) }
                    )
                }
            }
        }

        if (state.errorMessage != null) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        state.errorMessage!!,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        items(state.listings) { listing ->
            val isFavorite = state.favoriteIds.contains(listing.id)
            ListingCard(
                listing = listing,
                isFavorite = isFavorite,
                onToggleFavorite = { viewModel.onToggleFavorite(listing.id) },
                onSelect = { viewModel.onListingSelected(listing) }
            )
        }
    }

    if (state.showCreateDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showCreateModal(false) },
            title = { Text("New Marketplace Listing") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.newTitle,
                        onValueChange = { viewModel.onNewTitleChanged(it) },
                        label = { Text("Title") }
                    )
                    OutlinedTextField(
                        value = state.newDescription,
                        onValueChange = { viewModel.onNewDescriptionChanged(it) },
                        label = { Text("Description") }
                    )
                    OutlinedTextField(
                        value = state.newPrice,
                        onValueChange = { viewModel.onNewPriceChanged(it) },
                        label = { Text("Price ($)") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.createNewListing() }) {
                    Text("Post Listing")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showCreateModal(false) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ListingCard(
    listing: Listing,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onSelect: () -> Unit
) {
    BulletinCard(modifier = Modifier.clickable(onClick = onSelect)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(listing.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                listing.price.formatted,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(4.dp))
        Text("Seller: ${listing.sellerName}", style = MaterialTheme.typography.bodySmall)
        Text("Category: ${listing.category.name}", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        Text(listing.description, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onToggleFavorite) {
                Text(if (isFavorite) "★ Saved" else "☆ Save")
            }
        }
    }
}
