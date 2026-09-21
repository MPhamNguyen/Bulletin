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
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem

@Composable
fun MarketplaceScreen(viewModel: MarketplaceViewModel) {
    val state by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionHeader(
                    title = "Campus Marketplace",
                    subtitle = "Browse, search, & discover student items on campus"
                )
            }

            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    label = { Text("Search listings...") },
                    singleLine = true,
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
                    items(MarketplaceCategory.entries.toTypedArray()) { category ->
                        FilterChip(
                            selected = state.selectedCategory == category,
                            onClick = { viewModel.onCategorySelected(category) },
                            label = { Text(category.name) }
                        )
                    }
                }
            }

            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (state.items.isEmpty() && !state.isLoading) {
                item {
                    BulletinCard {
                        Text(
                            text = "No marketplace listings found for your search.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(state.items) { item ->
                val isSaved = state.savedItemIds.contains(item.id)
                MarketplaceItemCard(
                    item = item,
                    isSaved = isSaved,
                    onCardClick = { viewModel.onListingClicked(item.id.value) },
                    onToggleSaved = { viewModel.toggleSaved(item.id) }
                )
            }
        }

        if (state.isDetailSheetOpen) {
            MarketplaceDetailBottomSheet(
                listing = state.selectedListing,
                isLoading = state.isDetailLoading,
                errorMessage = state.detailErrorMessage,
                isSaved = state.selectedListing?.let { state.savedItemIds.contains(it.id) } ?: false,
                onDismiss = { viewModel.dismissListingDetail() },
                onRetry = { viewModel.retryLoadListingDetail() },
                onToggleSave = {
                    state.selectedListing?.let { viewModel.toggleSaved(it.id) }
                }
            )
        }
    }
}

@Composable
private fun MarketplaceItemCard(
    item: MarketplaceItem,
    isSaved: Boolean,
    onCardClick: () -> Unit,
    onToggleSaved: () -> Unit
) {
    BulletinCard(
        modifier = Modifier.clickable { onCardClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = item.price.formatted,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Seller: ${item.sellerName} • Category: ${item.category.name}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(
                onClick = onToggleSaved,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isSaved) "★ Saved" else "☆ Save")
            }
        }
    }
}
