package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.core.common.formatRelativeTime
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.BulletinTextFieldDefaults
import com.jdrms.bulletin.core.designsystem.SectionHeader
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem

@Composable
fun MarketplaceScreen(
    viewModel: MarketplaceViewModel,
    userReportViewModel: MarketplaceUserReportViewModel,
    userBlockViewModel: MarketplaceUserBlockViewModel,
    onMessageSeller: suspend (listingId: String, sellerId: String, sellerName: String, content: String) -> Boolean =
        { _, _, _, _ -> false },
    hasExistingConversation: suspend (listingId: String, sellerId: String) -> Boolean =
        { _, _ -> false },
    onSeeChat: suspend (listingId: String, sellerId: String) -> Unit = { _, _ -> }
) {
    val state by viewModel.uiState.collectAsState()
    val reportState by userReportViewModel.uiState.collectAsState()
    val blockState by userBlockViewModel.uiState.collectAsState()
    var isReportScreenOpen by remember { mutableStateOf(false) }

    val flowState = MarketplaceSellerFlowUiState(state, reportState, blockState, isReportScreenOpen)
    val flowViewModels = MarketplaceSellerFlowViewModels(viewModel, userReportViewModel, userBlockViewModel)
    if (!MarketplaceSellerProfileFlow(flowState, flowViewModels) { isReportScreenOpen = it }) {
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
                        value = state.filters.query,
                        onValueChange = viewModel::onSearchQueryChanged,
                        label = { Text("Search by title or category") },
                        singleLine = true,
                        colors = BulletinTextFieldDefaults.colors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    MarketplaceFilters(
                        filters = state.filters,
                        onCategorySelected = viewModel::onCategorySelected,
                        onConditionSelected = viewModel::onConditionSelected
                    )
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
                if (state.items.isEmpty() && !state.isLoading && state.errorMessage == null) {
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
                items(items = state.items, key = { item -> item.id.value }) { item ->
                    MarketplaceItemCard(
                        item = item,
                        isBookmarked = item.id in state.bookmarkedItemIds,
                        onCardClick = { viewModel.onListingClicked(item.id.value) },
                        onToggleBookmark = { viewModel.toggleBookmark(item.id) }
                    )
                }
                if (state.nextCursor != null) {
                    item(key = "marketplace-pagination-trigger") {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            if (state.isLoadingMore) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            } else {
                                TextButton(onClick = viewModel::loadNextPage) { Text("Load more") }
                            }
                        }
                    }
                }
                state.errorMessage?.let { message ->
                    item {
                        BulletinCard {
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            TextButton(onClick = viewModel::retryListings) { Text("Retry") }
                        }
                    }
                }
                state.bookmarkErrorMessage?.let { message ->
                    item {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (state.detail.isOpen) {
                MarketplaceDetailBottomSheet(
                    state = MarketplaceDetailSheetState(
                        listing = state.detail.listing,
                        isLoading = state.detail.isLoading,
                        errorMessage = state.detail.errorMessage,
                        bookmarkErrorMessage = state.bookmarkErrorMessage,
                        isBookmarked = state.detail.listing?.let {
                            it.id in state.bookmarkedItemIds
                        } ?: false,
                        currentUserId = state.currentUserId
                    ),
                    actions = MarketplaceDetailSheetActions(
                        onDismiss = viewModel::dismissListingDetail,
                        onRetry = viewModel::retryLoadListingDetail,
                        onToggleBookmark = {
                            state.detail.listing?.let { viewModel.toggleBookmark(it.id) }
                        },
                        onSellerClick = viewModel::onSellerClicked,
                        onMessageSeller = onMessageSeller,
                        hasExistingConversation = hasExistingConversation,
                        onSeeChat = onSeeChat
                    )
                )
            }
        }
    }
}

@Composable
private fun MarketplaceSellerProfileFlow(
    state: MarketplaceSellerFlowUiState,
    viewModels: MarketplaceSellerFlowViewModels,
    onReportScreenOpen: (Boolean) -> Unit
): Boolean {
    if (state.isReportScreenOpen) {
        ReportMarketplaceUserScreen(
            state = state.reportState,
            onReasonSelected = viewModels.report::selectReason,
            onDescriptionChanged = viewModels.report::updateDescription,
            onSubmit = viewModels.report::submit,
            onBack = { onReportScreenOpen(false) }
        )
        return true
    }
    if (!state.marketplaceState.isSellerProfileOpen) return false

    SellerProfileScreen(
        profile = state.marketplaceState.selectedSellerProfile,
        isLoading = state.marketplaceState.isSellerProfileLoading,
        errorMessage = state.marketplaceState.sellerProfileErrorMessage,
        onBack = viewModels.marketplace::dismissSellerProfile,
        onReportUser = {
            state.marketplaceState.selectedSellerProfile?.sellerId?.let(viewModels.report::begin)
            onReportScreenOpen(state.marketplaceState.selectedSellerProfile != null)
        },
        onBlockUser = viewModels.block::submit,
        blockState = SellerProfileBlockState(
            errorMessage = state.blockState.errorMessage,
            isBlocking = state.blockState.isBlocking
        )
    )
    LaunchedEffect(state.marketplaceState.isSellerProfileOpen, state.marketplaceState.selectedSellerProfile?.sellerId) {
        state.marketplaceState.selectedSellerProfile?.sellerId?.let(viewModels.block::begin)
            ?: viewModels.block.reset()
    }
    LaunchedEffect(state.blockState.blocked, state.blockState.blockedUserId) {
        if (state.blockState.blocked) {
            viewModels.marketplace.dismissListingDetail()
            viewModels.marketplace.dismissSellerProfile()
            viewModels.marketplace.refreshListings()
        }
    }
    return true
}

private data class MarketplaceSellerFlowUiState(
    val marketplaceState: MarketplaceUiState,
    val reportState: MarketplaceUserReportUiState,
    val blockState: MarketplaceUserBlockUiState,
    val isReportScreenOpen: Boolean
)

private data class MarketplaceSellerFlowViewModels(
    val marketplace: MarketplaceViewModel,
    val report: MarketplaceUserReportViewModel,
    val block: MarketplaceUserBlockViewModel
)

@Composable
private fun MarketplaceItemCard(
    item: MarketplaceItem,
    isBookmarked: Boolean,
    onCardClick: () -> Unit,
    onToggleBookmark: () -> Unit
) {
    BulletinCard(modifier = Modifier.clickable(onClick = onCardClick)) {
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
        Text(
            text = formatRelativeTime(item.createdAtMillis, currentTimeMillis()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            OutlinedButton(onClick = onToggleBookmark) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (isBookmarked) "Bookmarked" else "Bookmark",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isBookmarked) "Bookmarked" else "Bookmark")
            }
        }
    }
}
