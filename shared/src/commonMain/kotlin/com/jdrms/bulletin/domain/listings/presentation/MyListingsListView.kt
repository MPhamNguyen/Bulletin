package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import bulletin.shared.generated.resources.Res
import bulletin.shared.generated.resources.ic_arrow_back
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingStatus
import org.jetbrains.compose.resources.painterResource

@Composable
fun MyListingsListView(
    uiState: ListingsUiState,
    onBack: () -> Unit,
    onEditListing: (Listing) -> Unit,
    onDeleteListing: (Listing) -> Unit,
    onMarkSoldListing: (Listing) -> Unit,
    onRestoreListing: (Listing) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        MyListingsTopBar(
            activeCount = uiState.myListings.count { it.status == ListingStatus.AVAILABLE },
            onBack = onBack
        )

        uiState.errorMessage?.let { error ->
            MyListingsErrorBanner(error = error)
        }

        when {
            uiState.loadState == ListingsLoadState.LOADING && uiState.myListings.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            uiState.loadState == ListingsLoadState.LOADED && uiState.myListings.isEmpty() -> {
                MyListingsEmptyState()
            }
            uiState.myListings.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(BulletinSpacing.xl),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Unable to load listings. Please try again.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = BulletinSpacing.l, vertical = BulletinSpacing.s),
                    verticalArrangement = Arrangement.spacedBy(BulletinSpacing.m)
                ) {
                    items(uiState.myListings, key = { it.id.value }) { listing ->
                        MyListingItemCard(
                            listing = listing,
                            onEdit = { onEditListing(listing) },
                            onDelete = { onDeleteListing(listing) },
                            onMarkSold = { onMarkSoldListing(listing) },
                            onRestore = { onRestoreListing(listing) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(BulletinSpacing.l))
                    }
                }
            }
        }
    }
}

@Composable
private fun MyListingsTopBar(
    activeCount: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = BulletinSpacing.l, vertical = BulletinSpacing.m),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                painter = painterResource(Res.drawable.ic_arrow_back),
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.width(BulletinSpacing.s))
        Column {
            Text(
                text = "My Listings",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "$activeCount active items",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MyListingsErrorBanner(
    error: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = BulletinSpacing.l, vertical = BulletinSpacing.xs),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Text(
            text = error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(BulletinSpacing.m)
        )
    }
}

@Composable
private fun MyListingsEmptyState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(BulletinSpacing.xl),
        contentAlignment = Alignment.Center
    ) {
        BulletinCard {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(BulletinSpacing.s)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(BulletinSpacing.m))
                Text(
                    text = "No Listings Yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "You have not posted any listings. Use the '+' tab to create your first item!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = BulletinSpacing.s)
                )
            }
        }
    }
}
