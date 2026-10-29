package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jdrms.bulletin.core.designsystem.MessageSellerQuickActionActions
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceSellerProfile

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
    var sellerNavigation by remember {
        mutableStateOf(
            MarketplaceSellerNavigationState(
                destination = if (navigation.sellerProfileIdToOpen != null) {
                    MarketplaceDestination.SELLER_PROFILE
                } else {
                    MarketplaceDestination.CATALOG
                }
            )
        )
    }
    var sellerProfileOpenedFromInbox by remember { mutableStateOf(navigation.sellerProfileIdToOpen != null) }

    LaunchedEffect(navigation.sellerProfileIdToOpen) {
        navigation.sellerProfileIdToOpen?.let { sellerId ->
            sellerNavigation = sellerNavigation.navigate(MarketplaceDestination.SELLER_PROFILE)
            sellerProfileOpenedFromInbox = true
            sellerProfileViewModel.load(sellerId)
            navigation.onSellerProfileRequestHandled()
        }
    }

    LaunchedEffect(navigation.userReportIdToOpen) {
        navigation.userReportIdToOpen?.let { userId ->
            sellerProfileOpenedFromInbox = true
            sellerProfileViewModel.load(userId)
            userReportViewModel.begin(userId)
            sellerNavigation = sellerNavigation.navigate(MarketplaceDestination.REPORT_USER)
            navigation.onUserReportRequestHandled()
        }
    }

    val flowState = MarketplaceSellerFlowUiState(state, sellerProfileState, reportState, blockState, sellerNavigation)
    val flowViewModels = MarketplaceSellerFlowViewModels(
        viewModel,
        sellerProfileViewModel,
        userReportViewModel,
        userBlockViewModel,
        listingReportViewModel
    )
    val navigate = { destination: MarketplaceDestination ->
        sellerNavigation = sellerNavigation.navigate(destination)
    }
    if (!MarketplaceSellerProfileFlow(
            flowState,
            flowViewModels,
            actions,
            onOpenListing = { listingId ->
                val context = sellerProfileState.listings
                    .firstOrNull { it.id.value == listingId }
                    ?.toSellerListingContext()
                viewModel.onListingClicked(listingId)
                sellerNavigation = sellerNavigation.navigate(MarketplaceDestination.SELLER_LISTING, context)
            },
            onNavigate = navigate,
            onBackFromSellerProfile = {
                if (sellerProfileOpenedFromInbox) {
                    sellerProfileOpenedFromInbox = false
                    navigation.onReturnToInboxChat()
                } else {
                    navigate(MarketplaceDestination.CATALOG)
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
            onNavigate = { destination ->
                val context = state.detail.listing
                    ?.takeIf { destination == MarketplaceDestination.SELLER_PROFILE }
                    ?.toSellerListingContext()
                if (destination == MarketplaceDestination.SELLER_PROFILE) sellerProfileOpenedFromInbox = false
                sellerNavigation = sellerNavigation.navigate(destination, context)
            }
        )
    }
}

@Composable
private fun MarketplaceSellerProfileFlow(
    state: MarketplaceSellerFlowUiState,
    viewModels: MarketplaceSellerFlowViewModels,
    actions: MarketplaceScreenActions,
    onOpenListing: (String) -> Unit,
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
    if (!state.destination.showsSellerProfile) return false

    SellerProfileScreen(
        state = state.sellerProfileState,
        messageActions = state.messageActions(actions),
        listingActions = SellerProfileListingActions(
            onListingClick = onOpenListing,
            onCategorySelected = viewModels.sellerProfile::onCategorySelected,
            onConditionSelected = viewModels.sellerProfile::onConditionSelected
        ),
        onBack = onBackFromSellerProfile,
        onReportUser = {
            state.sellerProfileState.profile?.sellerId?.let(viewModels.report::begin)
            if (state.sellerProfileState.profile != null) onNavigate(MarketplaceDestination.REPORT_USER)
        },
        onBlockUser = viewModels.block::submit,
        options = SellerProfileScreenOptions(
            blockState = SellerProfileBlockState(
                errorMessage = state.blockState.errorMessage,
                isBlocking = state.blockState.isBlocking
            )
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
    val navigation: MarketplaceSellerNavigationState
) {
    val destination: MarketplaceDestination
        get() = navigation.destination

    fun messageActions(actions: MarketplaceScreenActions): MessageSellerQuickActionActions? {
        val profile = sellerProfileState.profile ?: return null
        return sellerProfileMessageActions(
            marketplaceState.currentUserId,
            profile,
            navigation.listingContext,
            actions
        )
    }
}

internal fun sellerProfileMessageActions(
    currentUserId: String?,
    profile: MarketplaceSellerProfile,
    listing: SellerListingContext?,
    actions: MarketplaceScreenActions
): MessageSellerQuickActionActions? {
    listing ?: return null
    if (!canMessageMarketplaceSeller(currentUserId, profile.sellerId, listing.sellerId)) return null
    return MessageSellerQuickActionActions(
        conversationKey = "${listing.listingId}:${profile.sellerId}",
        hasExistingConversation = { actions.hasExistingConversation(listing.listingId, profile.sellerId) },
        onSend = { content ->
            actions.onMessageSeller(listing.listingId, profile.sellerId, profile.name ?: listing.sellerName, content)
        },
        onSent = {},
        onSeeChat = { actions.onSeeChat(listing.listingId, profile.sellerId) }
    )
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
