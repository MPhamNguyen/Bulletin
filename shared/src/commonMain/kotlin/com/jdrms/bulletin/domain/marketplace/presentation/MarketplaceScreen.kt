package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jdrms.bulletin.core.designsystem.MessageSellerQuickActionActions

@Composable
fun MarketplaceScreen(
    viewModel: MarketplaceViewModel,
    sellerProfileViewModel: SellerProfileViewModel,
    userReportViewModel: MarketplaceUserReportViewModel,
    listingReportViewModel: ListingReportViewModel,
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
    val flowViewModels = MarketplaceSellerFlowViewModels(
        viewModel,
        sellerProfileViewModel,
        userReportViewModel,
        userBlockViewModel,
        listingReportViewModel
    )
    if (!MarketplaceSellerProfileFlow(flowState, flowViewModels, actions) { destination = it }) {
        MarketplaceCatalogContent(
            state = state,
            viewModel = viewModel,
            sellerProfileViewModel = sellerProfileViewModel,
            listingReportViewModel = listingReportViewModel,
            actions = actions,
            onContentNoLongerScrollable = onContentNoLongerScrollable,
            onNavigate = { destination = it }
        )
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
    if (!state.destination.showsSellerProfile) return false

    SellerProfileScreen(
        state = state.sellerProfileState,
        messageActions = state.messageActions(actions),
        listingActions = SellerProfileListingActions(
            onListingClick = { listingId ->
                viewModels.marketplace.onListingClicked(listingId)
                onNavigate(MarketplaceDestination.SELLER_LISTING)
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
    if (state.destination == MarketplaceDestination.SELLER_LISTING) {
        MarketplaceDetailFlow(
            state = state.marketplaceState,
            viewModel = viewModels.marketplace,
            sellerProfileViewModel = viewModels.sellerProfile,
            listingReportViewModel = viewModels.listingReport,
            actions = actions,
            onNavigate = onNavigate,
            onDetailDismiss = {
                viewModels.marketplace.dismissListingDetail()
                onNavigate(MarketplaceDestination.SELLER_PROFILE)
            }
        )
    }
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
    val block: MarketplaceUserBlockViewModel,
    val listingReport: ListingReportViewModel
)

internal enum class MarketplaceDestination {
    CATALOG,
    SELLER_PROFILE,
    SELLER_LISTING,
    REPORT_USER
}

private val MarketplaceDestination.showsSellerProfile: Boolean
    get() = this == MarketplaceDestination.SELLER_PROFILE || this == MarketplaceDestination.SELLER_LISTING

internal fun canMessageMarketplaceSeller(
    currentUserId: String?,
    profileSellerId: String,
    listingSellerId: String
): Boolean = currentUserId != null && currentUserId != profileSellerId && listingSellerId == profileSellerId
