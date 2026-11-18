package com.jdrms.bulletin.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Rect
import com.jdrms.bulletin.app.di.AppContainer
import com.jdrms.bulletin.app.navigation.AppDestination
import com.jdrms.bulletin.app.navigation.MutableMainNavigationState
import com.jdrms.bulletin.app.navigation.ProfileDestination
import com.jdrms.bulletin.app.navigation.backFromProfile
import com.jdrms.bulletin.domain.home.presentation.HomeScreen
import com.jdrms.bulletin.domain.listings.presentation.ListingsScreen
import com.jdrms.bulletin.domain.listings.presentation.ListingsViewModel
import com.jdrms.bulletin.domain.listings.presentation.MyListingsScreen
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceInterestsScreen
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceScreen
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceScreenActions
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceScreenNavigation
import com.jdrms.bulletin.domain.messages.presentation.MessagesScreen

@Composable
internal fun MainAppContent(
    content: MainAppContentState,
    onContentNoLongerScrollable: () -> Unit,
    onConversationVisibilityChanged: (Boolean) -> Unit,
    profileAnchorInWindow: Rect? = null,
    onSaveCelebration: (Float) -> Unit = {}
) {
    val container = content.container
    val navigation = content.navigation
    val models = content.models
    when (navigation.currentDestination) {
        AppDestination.HOME -> HomeScreen(
            models.home,
            profileAnchorInWindow = profileAnchorInWindow,
            onSaveCelebration = onSaveCelebration
        )
        AppDestination.MARKETPLACE -> MarketplaceDestination(container, navigation, models, onContentNoLongerScrollable)
        AppDestination.LISTINGS -> ListingsScreen(
            models.listings,
            onClose = {
                models.listings.clearMessages()
                navigation.closeCreateListing()
            }
        )
        AppDestination.MESSAGES -> MessagesScreen(
            viewModel = models.messages,
            onViewProfile = { sellerId ->
                navigation.pendingSellerProfileId = sellerId
                navigation.currentDestination = AppDestination.MARKETPLACE
            },
            onConversationVisibilityChanged = onConversationVisibilityChanged,
            onReportUser = { id ->
                navigation.pendingUserReportId = id
                navigation.currentDestination = AppDestination.MARKETPLACE
            },
            onViewListing = { listingId ->
                models.marketplace.onListingClicked(listingId)
                navigation.openListing(listingId)
            }
        )
        AppDestination.PROFILE -> ProfileDestinationContent(container, navigation, models, content.profile)
    }
}

@Composable
private fun MarketplaceDestination(
    container: AppContainer,
    navigation: MutableMainNavigationState,
    models: MainScaffoldModels,
    onContentNoLongerScrollable: () -> Unit
) {
    val conversationActions = ListingConversationActions(container.messages, navigation, models.messages)
    MarketplaceScreen(
        viewModel = models.marketplace,
        sellerProfileViewModel = models.sellerProfile,
        userReportViewModel = models.marketplaceUserReport,
        listingReportViewModel = models.listingReport,
        userBlockViewModel = models.marketplaceUserBlock,
        navigation = MarketplaceScreenNavigation(
            sellerProfileIdToOpen = navigation.pendingSellerProfileId,
            sellerReviewsIdToOpen = navigation.pendingSellerReviewsId,
            userReportIdToOpen = navigation.pendingUserReportId,
            onUserReportRequestHandled = { navigation.pendingUserReportId = null },
            onSellerProfileRequestHandled = { navigation.pendingSellerProfileId = null },
            onSellerReviewsRequestHandled = { navigation.pendingSellerReviewsId = null },
            listingIdToOpen = navigation.pendingListingId,
            onListingRequestHandled = { navigation.pendingListingId = null },
            onListingClosed = navigation::closeListing,
            onReturnToInboxChat = { navigation.currentDestination = AppDestination.MESSAGES },
            onContentNoLongerScrollable = onContentNoLongerScrollable
        ),
        actions = MarketplaceScreenActions(
            onMessageSeller = conversationActions::messageSeller,
            hasExistingConversation = conversationActions::hasExistingConversation,
            onSeeChat = conversationActions::seeChat,
            onMessageSent = {
                navigation.apply(navigation.snapshot().selectBottomNavigationDestination(AppDestination.MESSAGES))
            }
        )
    )
}

@Composable
private fun ProfileDestinationContent(
    container: AppContainer,
    navigation: MutableMainNavigationState,
    models: MainScaffoldModels,
    profilePresentation: ProfilePresentation
) {
    val interestState by profilePresentation.marketplaceInterestsViewModel.uiState.collectAsState()
    if (navigation.profileDestination == ProfileDestination.MY_LISTINGS ||
        navigation.profileDestination == ProfileDestination.EDIT_LISTING
    ) {
        MyListingsDestination(navigation, models.listings)
    } else if (navigation.profileDestination == ProfileDestination.PUBLIC_PROFILE) {
        PublicProfilePreviewDestination(
            profilePresentation.viewModel,
            models.sellerProfile,
            onListingClick = { listingId ->
                models.marketplace.onListingClicked(listingId)
                navigation.openListing(listingId)
            },
            onBack = { navigation.profileDestination = ProfileDestination.SETTINGS }
        )
    } else if (navigation.profileDestination == ProfileDestination.INTERESTS) {
        MarketplaceInterestsScreen(
            viewModel = profilePresentation.marketplaceInterestsViewModel,
            onBack = { navigation.profileDestination = ProfileDestination.PROFILE },
            onListingClick = { listingId ->
                models.marketplace.onListingClicked(listingId)
                navigation.openListing(listingId)
            }
        )
    } else {
        StudentProfileDestination(
            container,
            navigation,
            models,
            profilePresentation,
            interestState.unreadMatchCount
        )
    }
}

@Composable
private fun MyListingsDestination(navigation: MutableMainNavigationState, listingsViewModel: ListingsViewModel) {
    MyListingsScreen(
        viewModel = listingsViewModel,
        onBack = {
            listingsViewModel.clearMessages()
            listingsViewModel.cancelEditing()
            navigation.profileDestination = backFromProfile(navigation.profileDestination)
        },
        onEditListing = { navigation.profileDestination = ProfileDestination.EDIT_LISTING }
    )
}
