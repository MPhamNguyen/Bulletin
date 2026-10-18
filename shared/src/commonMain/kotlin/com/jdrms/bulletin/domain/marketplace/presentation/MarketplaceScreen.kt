package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.jdrms.bulletin.core.designsystem.MessageSellerQuickActionActions
import com.jdrms.bulletin.core.designsystem.SectionHeader

@Composable
fun MarketplaceScreen(
    viewModel: MarketplaceViewModel,
    sellerProfileViewModel: SellerProfileViewModel,
    userReportViewModel: MarketplaceUserReportViewModel,
    listingReportViewModel: ListingReportViewModel,
    userBlockViewModel: MarketplaceUserBlockViewModel,
    actions: MarketplaceScreenActions = MarketplaceScreenActions()
) {
    val state by viewModel.uiState.collectAsState()
    val sellerProfileState by sellerProfileViewModel.uiState.collectAsState()
    val reportState by userReportViewModel.uiState.collectAsState()
    val blockState by userBlockViewModel.uiState.collectAsState()
    var destination by remember { mutableStateOf(MarketplaceDestination.CATALOG) }

    val flowState = MarketplaceSellerFlowUiState(state, sellerProfileState, reportState, blockState, destination)
    val flowViewModels = MarketplaceSellerFlowViewModels(
        viewModel,
        sellerProfileViewModel,
        userReportViewModel,
        userBlockViewModel
    )
    if (!MarketplaceSellerProfileFlow(flowState, flowViewModels, actions) { destination = it }) {
        MarketplaceCatalogContent(
            state = state,
            viewModel = viewModel,
            sellerProfileViewModel = sellerProfileViewModel,
            listingReportViewModel = listingReportViewModel,
            actions = actions,
            onNavigate = { destination = it }
        )
    }
}

@Composable
private fun MarketplaceCatalogContent(
    state: MarketplaceUiState,
    viewModel: MarketplaceViewModel,
    sellerProfileViewModel: SellerProfileViewModel,
    listingReportViewModel: ListingReportViewModel,
    actions: MarketplaceScreenActions,
    onNavigate: (MarketplaceDestination) -> Unit
) {
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
                    onReportListing = {
                        beginMarketplaceListingReport(listingReportViewModel, state.detail.listing)
                    },
                    onSellerClick = { sellerId ->
                        sellerProfileViewModel.load(sellerId)
                        onNavigate(MarketplaceDestination.SELLER_PROFILE)
                    },
                    onMessageSeller = actions.onMessageSeller,
                    hasExistingConversation = actions.hasExistingConversation,
                    onSeeChat = actions.onSeeChat
                )
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
        listingActions = SellerProfileListingActions(
            onListingClick = { listingId ->
                onNavigate(MarketplaceDestination.CATALOG)
                viewModels.marketplace.onListingClicked(listingId)
            },
            onCategorySelected = viewModels.sellerProfile::onCategorySelected,
            onConditionSelected = viewModels.sellerProfile::onConditionSelected
        ),
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
    val sellerProfile: SellerProfileViewModel,
    val report: MarketplaceUserReportViewModel,
    val block: MarketplaceUserBlockViewModel
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
