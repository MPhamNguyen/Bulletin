package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
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
    val focusManager = LocalFocusManager.current
    var searchBounds by remember { mutableStateOf(Rect.Zero) }
    var containerOrigin by remember { mutableStateOf(Offset.Zero) }
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { containerOrigin = it.positionInRoot() }
            .pointerInput(focusManager, searchBounds, containerOrigin) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!searchBounds.contains(down.position + containerOrigin)) focusManager.clearFocus()
                }
            }
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(state.layoutPreference.columns),
            state = gridState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = LocalNavbarHeight.current),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            marketplaceCatalogHeader(state, viewModel) { coordinates ->
                searchBounds = Rect(coordinates.positionInRoot(), coordinates.size.toSize())
            }
            marketplaceLoadingItem(state.isLoading)
            marketplaceEmptyResultsItem(state)
            marketplaceListingItems(state, viewModel)
            marketplacePaginationItem(state)
            marketplaceErrorItems(state, viewModel)
        }
        MarketplaceCatalogOverlays(
            state,
            viewModel,
            sellerProfileViewModel,
            listingReportViewModel,
            actions,
            onNavigate
        )
    }
}

private fun LazyGridScope.marketplaceCatalogHeader(
    state: MarketplaceUiState,
    viewModel: MarketplaceViewModel,
    onSearchPositioned: (androidx.compose.ui.layout.LayoutCoordinates) -> Unit
) {
    fullWidthItem {
        SectionHeader(
            title = "Campus Marketplace",
            subtitle = "Browse, search, & discover student items on campus"
        )
    }
    fullWidthItem {
        MarketplaceSearchFilters(
            filters = state.filters,
            onQueryChanged = viewModel::onSearchQueryChanged,
            onFiltersApplied = viewModel::onFiltersApplied,
            onSearchPositioned = onSearchPositioned
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
private fun MarketplaceCatalogOverlays(
    state: MarketplaceUiState,
    viewModel: MarketplaceViewModel,
    sellerProfileViewModel: SellerProfileViewModel,
    listingReportViewModel: ListingReportViewModel,
    actions: MarketplaceScreenActions,
    onNavigate: (MarketplaceDestination) -> Unit
) {
    if (!state.detail.isOpen) return
    MarketplaceDetailOverlay(
        state = state,
        viewModel = viewModel,
        sellerProfileViewModel = sellerProfileViewModel,
        listingReportViewModel = listingReportViewModel,
        actions = actions,
        onNavigate = onNavigate
    )
    MarketplaceListingReportFlow(listingReportViewModel) {
        listingReportViewModel.dismiss()
        viewModel.dismissListingDetail()
        viewModel.refreshListings()
    }
    if (state.detail.isReviewDialogOpen) {
        MarketplaceReviewDialog(
            isSubmitting = state.detail.isSubmittingReview,
            errorMessage = state.detail.reviewErrorMessage,
            onDismiss = viewModel.reviewActions::dismissReviewDialog,
            onSubmit = viewModel.reviewActions::submitReview
        )
    }
}

@Composable
private fun MarketplaceDetailOverlay(
    state: MarketplaceUiState,
    viewModel: MarketplaceViewModel,
    sellerProfileViewModel: SellerProfileViewModel,
    listingReportViewModel: ListingReportViewModel,
    actions: MarketplaceScreenActions,
    onNavigate: (MarketplaceDestination) -> Unit
) {
    MarketplaceDetailBottomSheet(
        state = MarketplaceDetailSheetState(
            listing = state.detail.listing,
            isLoading = state.detail.isLoading,
            errorMessage = state.detail.errorMessage,
            bookmarkErrorMessage = state.bookmarkErrorMessage,
            isBookmarked = state.detail.listing?.let { it.id in state.bookmarkedItemIds } ?: false,
            currentUserId = state.currentUserId,
            hasReview = state.detail.hasReview
        ),
        actions = MarketplaceDetailSheetActions(
            onDismiss = viewModel::dismissListingDetail,
            onRetry = viewModel::retryLoadListingDetail,
            onToggleBookmark = { state.detail.listing?.let { viewModel.toggleBookmark(it.id) } },
            onLeaveReview = viewModel.reviewActions::openReviewDialog,
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
