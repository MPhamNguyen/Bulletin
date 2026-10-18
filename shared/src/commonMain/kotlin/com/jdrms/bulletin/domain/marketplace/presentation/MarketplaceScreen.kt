package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.BulletinTextFieldDefaults
import com.jdrms.bulletin.core.designsystem.LocalNavbarHeight
import com.jdrms.bulletin.core.designsystem.MessageSellerQuickActionActions
import com.jdrms.bulletin.core.designsystem.SectionHeader

@Composable
fun MarketplaceScreen(
    viewModel: MarketplaceViewModel,
    sellerProfileViewModel: SellerProfileViewModel,
    userReportViewModel: MarketplaceUserReportViewModel,
    userBlockViewModel: MarketplaceUserBlockViewModel,
    actions: MarketplaceScreenActions = MarketplaceScreenActions(),
    onContentNoLongerScrollable: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val sellerProfileState by sellerProfileViewModel.uiState.collectAsState()
    val reportState by userReportViewModel.uiState.collectAsState()
    val blockState by userBlockViewModel.uiState.collectAsState()
    var destination by remember { mutableStateOf(MarketplaceDestination.CATALOG) }

    val flowState = MarketplaceSellerFlowUiState(state, sellerProfileState, reportState, blockState, destination)
    val flowViewModels = MarketplaceSellerFlowViewModels(viewModel, userReportViewModel, userBlockViewModel)
    if (!MarketplaceSellerProfileFlow(flowState, flowViewModels, actions) { destination = it }) {
        MarketplaceCatalogContent(
            state = state,
            viewModel = viewModel,
            sellerProfileViewModel = sellerProfileViewModel,
            actions = actions,
            onContentNoLongerScrollable = onContentNoLongerScrollable,
            onNavigate = { destination = it }
        )
    }
}

@Composable
private fun MarketplaceCatalogContent(
    state: MarketplaceUiState,
    viewModel: MarketplaceViewModel,
    sellerProfileViewModel: SellerProfileViewModel,
    actions: MarketplaceScreenActions,
    onContentNoLongerScrollable: () -> Unit,
    onNavigate: (MarketplaceDestination) -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState.canScrollForward) {
        if (!listState.canScrollForward) onContentNoLongerScrollable()
    }
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = LocalNavbarHeight.current),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            marketplaceCatalogHeader(state, viewModel)
            marketplaceLoadingItem(state.isLoading)
            marketplaceEmptyResultsItem(state)
            marketplaceListingItems(state, viewModel)
            marketplacePaginationItem(state, viewModel)
            marketplaceErrorItems(state, viewModel)
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
                    currentUserId = state.currentUserId,
                    hasReview = state.detail.hasReview
                ),
                actions = MarketplaceDetailSheetActions(
                    onDismiss = viewModel::dismissListingDetail,
                    onRetry = viewModel::retryLoadListingDetail,
                    onToggleBookmark = {
                        state.detail.listing?.let { viewModel.toggleBookmark(it.id) }
                    },
                    onLeaveReview = viewModel.reviewActions::openReviewDialog,
                    onSellerClick = { sellerId ->
                        sellerProfileViewModel.load(sellerId)
                        onNavigate(MarketplaceDestination.SELLER_PROFILE)
                    },
                    onMessageSeller = actions.onMessageSeller,
                    hasExistingConversation = actions.hasExistingConversation,
                    onSeeChat = actions.onSeeChat
                )
            )
            if (state.detail.isReviewDialogOpen) {
                MarketplaceReviewDialog(
                    isSubmitting = state.detail.isSubmittingReview,
                    errorMessage = state.detail.reviewErrorMessage,
                    onDismiss = viewModel.reviewActions::dismissReviewDialog,
                    onSubmit = viewModel.reviewActions::submitReview
                )
            }
        }
    }
}

private fun LazyListScope.marketplaceCatalogHeader(state: MarketplaceUiState, viewModel: MarketplaceViewModel) {
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
}

private fun LazyListScope.marketplaceLoadingItem(isLoading: Boolean) {
    if (!isLoading) return
    item {
        Box(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun LazyListScope.marketplaceEmptyResultsItem(state: MarketplaceUiState) {
    if (state.items.isNotEmpty() || state.isLoading || state.errorMessage != null) return
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

private fun LazyListScope.marketplaceListingItems(state: MarketplaceUiState, viewModel: MarketplaceViewModel) {
    items(items = state.items, key = { item -> item.id.value }) { item ->
        MarketplaceItemCard(
            item = item,
            isBookmarked = item.id in state.bookmarkedItemIds,
            onCardClick = { viewModel.onListingClicked(item.id.value) },
            onToggleBookmark = { viewModel.toggleBookmark(item.id) }
        )
    }
}

private fun LazyListScope.marketplacePaginationItem(state: MarketplaceUiState, viewModel: MarketplaceViewModel) {
    if (state.nextCursor == null) return
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

private fun LazyListScope.marketplaceErrorItems(state: MarketplaceUiState, viewModel: MarketplaceViewModel) {
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

@Composable
private fun MarketplaceSellerProfileFlow(
    state: MarketplaceSellerFlowUiState,
    viewModels: MarketplaceSellerFlowViewModels,
    actions: MarketplaceScreenActions,
    onNavigate: (MarketplaceDestination) -> Unit
): Boolean {
    if (state.destination == MarketplaceDestination.REPORT_USER) {
        ReportMarketplaceUserScreen(
            state = state.reportState,
            onReasonSelected = viewModels.report::selectReason,
            onDescriptionChanged = viewModels.report::updateDescription,
            onSubmit = viewModels.report::submit,
            onBack = { onNavigate(MarketplaceDestination.SELLER_PROFILE) }
        )
        return true
    }
    if (state.destination != MarketplaceDestination.SELLER_PROFILE) return false

    SellerProfileScreen(
        state = state.sellerProfileState,
        messageActions = state.messageActions(actions),
        onListingClick = { listingId ->
            onNavigate(MarketplaceDestination.CATALOG)
            viewModels.marketplace.onListingClicked(listingId)
        },
        onBack = { onNavigate(MarketplaceDestination.CATALOG) },
        onReportUser = {
            state.sellerProfileState.profile?.sellerId?.let(viewModels.report::begin)
            if (state.sellerProfileState.profile != null) onNavigate(MarketplaceDestination.REPORT_USER)
        },
        onBlockUser = viewModels.block::submit,
        blockState = SellerProfileBlockState(
            errorMessage = state.blockState.errorMessage,
            isBlocking = state.blockState.isBlocking
        )
    )
    LaunchedEffect(state.sellerProfileState.profile?.sellerId) {
        state.sellerProfileState.profile?.sellerId?.let(viewModels.block::begin)
            ?: viewModels.block.reset()
    }
    LaunchedEffect(state.blockState.blocked, state.blockState.blockedUserId) {
        if (state.blockState.blocked) {
            viewModels.marketplace.dismissListingDetail()
            onNavigate(MarketplaceDestination.CATALOG)
            viewModels.marketplace.refreshListings()
        }
    }
    return true
}

private data class MarketplaceSellerFlowUiState(
    val marketplaceState: MarketplaceUiState,
    val sellerProfileState: SellerProfileUiState,
    val reportState: MarketplaceUserReportUiState,
    val blockState: MarketplaceUserBlockUiState,
    val destination: MarketplaceDestination
) {
    fun messageActions(actions: MarketplaceScreenActions): MessageSellerQuickActionActions? {
        val profile = sellerProfileState.profile ?: return null
        val listing = marketplaceState.detail.listing ?: return null
        val canMessage = canMessageMarketplaceSeller(
            marketplaceState.currentUserId,
            profile.sellerId,
            listing.sellerId
        )
        if (!canMessage) return null
        return MessageSellerQuickActionActions(
            conversationKey = "${listing.id.value}:${profile.sellerId}",
            hasExistingConversation = { actions.hasExistingConversation(listing.id.value, profile.sellerId) },
            onSend = { content ->
                actions.onMessageSeller(listing.id.value, profile.sellerId, profile.name ?: listing.sellerName, content)
            },
            onSent = {},
            onSeeChat = { actions.onSeeChat(listing.id.value, profile.sellerId) }
        )
    }
}

private data class MarketplaceSellerFlowViewModels(
    val marketplace: MarketplaceViewModel,
    val report: MarketplaceUserReportViewModel,
    val block: MarketplaceUserBlockViewModel
)

data class MarketplaceScreenActions(
    val onMessageSeller: suspend (String, String, String, String) -> Boolean = { _, _, _, _ -> false },
    val hasExistingConversation: suspend (String, String) -> Boolean = { _, _ -> false },
    val onSeeChat: suspend (String, String) -> Unit = { _, _ -> }
)

private enum class MarketplaceDestination {
    CATALOG,
    SELLER_PROFILE,
    REPORT_USER
}

internal fun canMessageMarketplaceSeller(
    currentUserId: String?,
    profileSellerId: String,
    listingSellerId: String
): Boolean = currentUserId != null && currentUserId != profileSellerId && listingSellerId == profileSellerId
