package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jdrms.bulletin.core.designsystem.MessageSellerQuickActionActions

data class MarketplaceScreenNavigation(
    val sellerProfileIdToOpen: String? = null,
    val onSellerProfileRequestHandled: () -> Unit = {},
    val onReturnToInboxChat: () -> Unit = {},
    val onContentNoLongerScrollable: () -> Unit = {}
)

@Composable
fun MarketplaceScreen(
    viewModel: MarketplaceViewModel,
    sellerProfileViewModel: SellerProfileViewModel,
    userReportViewModel: MarketplaceUserReportViewModel,
    listingReportViewModel: ListingReportViewModel,
    userBlockViewModel: MarketplaceUserBlockViewModel,
    actions: MarketplaceScreenActions = MarketplaceScreenActions(),
    navigation: MarketplaceScreenNavigation = MarketplaceScreenNavigation()
) {
    val state by viewModel.uiState.collectAsState()
    val sellerProfileState by sellerProfileViewModel.uiState.collectAsState()
    val reportState by userReportViewModel.uiState.collectAsState()
    val blockState by userBlockViewModel.uiState.collectAsState()
    var destination by remember {
        mutableStateOf(
            if (navigation.sellerProfileIdToOpen != null) {
                MarketplaceDestination.SELLER_PROFILE
            } else {
                MarketplaceDestination.CATALOG
            }
        )
    }
    var sellerProfileOpenedFromInbox by remember {
        mutableStateOf(navigation.sellerProfileIdToOpen != null)
    }

    LaunchedEffect(navigation.sellerProfileIdToOpen) {
        navigation.sellerProfileIdToOpen?.let { sellerId ->
            sellerProfileViewModel.load(sellerId)
            navigation.onSellerProfileRequestHandled()
        }
    }

    val flowState = MarketplaceSellerFlowUiState(state, sellerProfileState, reportState, blockState, destination)
    val flowViewModels = MarketplaceSellerFlowViewModels(
        viewModel,
        sellerProfileViewModel,
        userReportViewModel,
        userBlockViewModel
    )
    if (!MarketplaceSellerProfileFlow(
            flowState,
            flowViewModels,
            actions,
            onNavigate = { destination = it },
            onBackFromSellerProfile = {
                if (sellerProfileOpenedFromInbox) {
                    sellerProfileOpenedFromInbox = false
                    navigation.onReturnToInboxChat()
                } else {
                    destination = MarketplaceDestination.CATALOG
                }
            }
        )
    ) {
        MarketplaceCatalogContent(
            state = state,
            viewModel = viewModel,
            sellerProfileViewModel = sellerProfileViewModel,
            listingReportViewModel = listingReportViewModel,
            actions = actions,
            onContentNoLongerScrollable = navigation.onContentNoLongerScrollable,
            onNavigate = {
                if (it == MarketplaceDestination.SELLER_PROFILE) sellerProfileOpenedFromInbox = false
                destination = it
            }
        )
    }
}

@Composable
private fun MarketplaceSellerProfileFlow(
    state: MarketplaceSellerFlowUiState,
    viewModels: MarketplaceSellerFlowViewModels,
    actions: MarketplaceScreenActions,
    onNavigate: (MarketplaceDestination) -> Unit,
    onBackFromSellerProfile: () -> Unit
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
        onBack = onBackFromSellerProfile,
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

internal enum class MarketplaceDestination {
    CATALOG,
    SELLER_PROFILE,
    REPORT_USER
}

internal fun canMessageMarketplaceSeller(
    currentUserId: String?,
    profileSellerId: String,
    listingSellerId: String
): Boolean = currentUserId != null && currentUserId != profileSellerId && listingSellerId == profileSellerId
