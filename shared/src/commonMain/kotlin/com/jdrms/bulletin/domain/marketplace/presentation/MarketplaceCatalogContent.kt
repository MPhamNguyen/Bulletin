package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.LocalNavbarHeight
import com.jdrms.bulletin.core.designsystem.SectionHeader
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun MarketplaceCatalogContent(
    state: MarketplaceUiState,
    viewModel: MarketplaceViewModel,
    sellerProfileViewModel: SellerProfileViewModel,
    listingReportViewModel: ListingReportViewModel,
    actions: MarketplaceScreenActions,
    onContentNoLongerScrollable: () -> Unit,
    onNavigate: (MarketplaceDestination) -> Unit
) {
    val gridState = rememberLazyGridState()
    LaunchedEffect(gridState.canScrollForward) {
        if (!gridState.canScrollForward) onContentNoLongerScrollable()
    }
    LaunchedEffect(
        gridState,
        state.nextCursor,
        state.isLoading,
        state.isLoadingMore,
        state.errorMessage,
        state.filters
    ) {
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            layoutInfo.totalItemsCount - lastVisibleIndex
        }.distinctUntilChanged().collect { remainingItems ->
            if (state.nextCursor == null) return@collect
            if (state.isLoading || state.isLoadingMore) return@collect
            if (state.errorMessage != null) return@collect
            if (remainingItems !in 1..PREFETCH_ITEM_THRESHOLD) return@collect
            viewModel.loadNextPage()
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(state.layoutPreference.columns),
            state = gridState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = LocalNavbarHeight.current),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            marketplaceCatalogHeader(state, viewModel)
            marketplaceLoadingItem(state.isLoading)
            marketplaceEmptyResultsItem(state)
            marketplaceListingItems(state, viewModel)
            marketplacePaginationItem(state)
            marketplaceErrorItems(state, viewModel)
        }
        MarketplaceDetailFlow(
            state,
            viewModel,
            sellerProfileViewModel,
            listingReportViewModel,
            actions,
            onNavigate,
            viewModel::dismissListingDetail
        )
    }
}

private fun LazyGridScope.marketplaceCatalogHeader(
    state: MarketplaceUiState,
    viewModel: MarketplaceViewModel
) {
    fullWidthItem {
        SectionHeader(
            title = "Marketplace"
        )
    }
    fullWidthItem {
        MarketplaceSearchFilters(
            filters = state.filters,
            onQueryChanged = viewModel::onSearchQueryChanged,
            onFiltersApplied = viewModel::onFiltersApplied
        )
    }
    fullWidthItem {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MarketplaceLayoutToggle(state.layoutPreference, viewModel::onLayoutPreferenceChanged)
        }
    }
}

private fun LazyGridScope.marketplaceLoadingItem(isLoading: Boolean) {
    if (!isLoading) return
    fullWidthItem {
        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun LazyGridScope.marketplaceEmptyResultsItem(state: MarketplaceUiState) {
    if (state.items.isNotEmpty() || state.isLoading || state.errorMessage != null) return
    fullWidthItem {
        BulletinCard {
            Text(
                text = "No marketplace listings found for your search.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun LazyGridScope.marketplaceListingItems(state: MarketplaceUiState, viewModel: MarketplaceViewModel) {
    items(items = state.items, key = { item -> item.id.value }) { item ->
        MarketplaceItemCard(
            item = item,
            columns = state.layoutPreference.columns,
            isBookmarked = item.id in state.bookmarkedItemIds,
            onBookmarkClick = { viewModel.toggleBookmark(item.id) },
            onCardClick = { viewModel.onListingClicked(item.id.value) }
        )
    }
}

private fun LazyGridScope.marketplacePaginationItem(state: MarketplaceUiState) {
    if (state.nextCursor == null) return
    fullWidthItem(key = "marketplace-pagination-trigger") {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (state.isLoadingMore) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun LazyGridScope.marketplaceErrorItems(state: MarketplaceUiState, viewModel: MarketplaceViewModel) {
    state.errorMessage?.let { message ->
        fullWidthItem {
            BulletinCard {
                Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = viewModel::retryListings) { Text("Retry") }
            }
        }
    }
    state.bookmarkErrorMessage?.let { message ->
        fullWidthItem {
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
    }
}

private fun LazyGridScope.fullWidthItem(
    key: Any? = null,
    content: @Composable () -> Unit
) = item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }

private const val PREFETCH_ITEM_THRESHOLD = 6

@Composable
internal fun MarketplaceDetailFlow(
    state: MarketplaceUiState,
    viewModel: MarketplaceViewModel,
    sellerProfileViewModel: SellerProfileViewModel,
    listingReportViewModel: ListingReportViewModel,
    actions: MarketplaceScreenActions,
    onNavigate: (MarketplaceDestination) -> Unit,
    onDetailDismiss: () -> Unit
) {
    if (!state.detail.isOpen) return
    MarketplaceDetailOverlay(
        state = state,
        viewModel = viewModel,
        sellerProfileViewModel = sellerProfileViewModel,
        listingReportViewModel = listingReportViewModel,
        actions = actions,
        onNavigate = onNavigate,
        onDetailDismiss = onDetailDismiss
    )
    MarketplaceListingReportFlow(listingReportViewModel) {
        listingReportViewModel.dismiss()
        onDetailDismiss()
        viewModel.refreshListings()
    }
}

@Composable
private fun MarketplaceDetailOverlay(
    state: MarketplaceUiState,
    viewModel: MarketplaceViewModel,
    sellerProfileViewModel: SellerProfileViewModel,
    listingReportViewModel: ListingReportViewModel,
    actions: MarketplaceScreenActions,
    onNavigate: (MarketplaceDestination) -> Unit,
    onDetailDismiss: () -> Unit
) {
    MarketplaceDetailBottomSheet(
        state = MarketplaceDetailSheetState(
            listing = state.detail.listing,
            isLoading = state.detail.isLoading,
            errorMessage = state.detail.errorMessage,
            bookmarkErrorMessage = state.bookmarkErrorMessage,
            isBookmarked = state.detail.listing?.let { it.id in state.bookmarkedItemIds } ?: false,
            currentUserId = state.currentUserId,
            hasReview = state.detail.hasReview,
            isReviewDialogOpen = state.detail.isReviewDialogOpen,
            isSubmittingReview = state.detail.isSubmittingReview,
            reviewErrorMessage = state.detail.reviewErrorMessage
        ),
        actions = MarketplaceDetailSheetActions(
            onDismiss = onDetailDismiss,
            onRetry = viewModel::retryLoadListingDetail,
            onToggleBookmark = { state.detail.listing?.let { viewModel.toggleBookmark(it.id) } },
            onLeaveReview = viewModel.reviewActions::openReviewDialog,
            onDismissReview = viewModel.reviewActions::dismissReviewDialog,
            onSubmitReview = viewModel.reviewActions::submitReview,
            onReportListing = { beginMarketplaceListingReport(listingReportViewModel, state.detail.listing) },
            onSellerClick = { sellerId ->
                sellerProfileViewModel.load(sellerId)
                onNavigate(MarketplaceDestination.SELLER_PROFILE)
            },
            onMessageSeller = actions.onMessageSeller,
            hasExistingConversation = actions.hasExistingConversation,
            onSeeChat = actions.onSeeChat
        )
    )
}
