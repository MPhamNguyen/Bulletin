package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListing

@Composable
internal fun BookmarkedListingsView(
    state: BookmarkedListingsViewState,
    actions: BookmarkedListingsActions
) {
    val listings = state.listings
    val isLoading = state.isLoading
    val isLoadingMore = state.isLoadingMore
    val hasMore = state.hasMore
    val errorMessage = state.errorMessage
    val removingListingId = state.removingListingId
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        BookmarkedListingsTopBar(
            onBackClick = actions.onBack
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = BulletinSpacing.l, vertical = BulletinSpacing.m),
            verticalArrangement = Arrangement.spacedBy(BulletinSpacing.m)
        ) {
            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (errorMessage != null) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        if (listings.isEmpty()) {
                            BulletinButton(
                                onClick = actions.onRefresh,
                                variant = BulletinButtonVariant.Text
                            ) { Text("Retry") }
                        }
                    }
                }
            }

            if (state.allListings.isNotEmpty()) {
                item { BookmarkedListingFilters(state, actions) }
            }

            if (state.shouldShowEmptyState) {
                item { EmptyBookmarksCard(hasBookmarks = state.allListings.isNotEmpty()) }
            }

            items(items = listings, key = ProfileBookmarkedListing::id) { listing ->
                BookmarkedListingCard(
                    listing = listing,
                    isRemoving = removingListingId == listing.id,
                    bookmarkActionEnabled = removingListingId == null,
                    onClick = { actions.onListingClick(listing.id) },
                    onRemoveBookmark = { actions.onRemoveBookmark(listing.id) }
                )
            }

            if (hasMore || isLoadingMore) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        BulletinButton(
                            onClick = actions.onLoadMore,
                            enabled = !isLoadingMore,
                            variant = BulletinButtonVariant.Outlined
                        ) {
                            if (isLoadingMore) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(BulletinSpacing.s))
                            }
                            Text(if (isLoadingMore) "Loading..." else "Load More")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyBookmarksCard(hasBookmarks: Boolean) {
    BulletinCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = BulletinSpacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.BookmarkBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(BulletinSpacing.l))
            Text(
                text = if (hasBookmarks) "No matching listings" else "Bookmarked Listings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(BulletinSpacing.xs))
            Text(
                text = if (hasBookmarks) {
                    "Try changing the category or condition filters."
                } else {
                    "Listings you bookmark will appear here."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BookmarkedListingsTopBar(
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = BulletinSpacing.l, vertical = BulletinSpacing.m),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back to Profile",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.width(BulletinSpacing.s))
        Text(
            text = "Bookmarked Listings",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
    }
}
