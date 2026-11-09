package com.jdrms.bulletin.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.app.di.AppContainer
import com.jdrms.bulletin.app.di.createAccountViewModel
import com.jdrms.bulletin.app.di.createBlockedUsersViewModel
import com.jdrms.bulletin.app.di.createEditProfileViewModel
import com.jdrms.bulletin.app.navigation.AppDestination
import com.jdrms.bulletin.app.navigation.MutableMainNavigationState
import com.jdrms.bulletin.app.navigation.ProfileDestination
import com.jdrms.bulletin.app.navigation.backFromProfile
import com.jdrms.bulletin.domain.home.presentation.HomeScreen
import com.jdrms.bulletin.domain.listings.presentation.ListingsScreen
import com.jdrms.bulletin.domain.listings.presentation.ListingsViewModel
import com.jdrms.bulletin.domain.listings.presentation.MyListingsScreen
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceReporterId
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceScreen
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceScreenActions
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceScreenNavigation
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceViewModel
import com.jdrms.bulletin.domain.marketplace.presentation.SellerProfileViewModel
import com.jdrms.bulletin.domain.messages.presentation.MessagesScreen
import com.jdrms.bulletin.domain.messages.presentation.MessagesViewModel
import com.jdrms.bulletin.domain.profile.presentation.ProfileScreen
import com.jdrms.bulletin.domain.profile.presentation.ProfileScreenFlows
import com.jdrms.bulletin.domain.profile.presentation.ReportedListingVisibilitySetting
import kotlinx.coroutines.cancel

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
            }
        )
        AppDestination.PROFILE -> ProfileDestinationContent(
            container,
            navigation,
            models.listings,
            models.marketplace,
            models.sellerProfile,
            content.profile,
            models.messages
        )
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
            userReportIdToOpen = navigation.pendingUserReportId,
            onUserReportRequestHandled = { navigation.pendingUserReportId = null },
            onSellerProfileRequestHandled = { navigation.pendingSellerProfileId = null },
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
    listingsViewModel: ListingsViewModel,
    marketplaceViewModel: MarketplaceViewModel,
    sellerProfileViewModel: SellerProfileViewModel,
    profilePresentation: ProfilePresentation,
    messagesViewModel: MessagesViewModel
) {
    if (navigation.profileDestination == ProfileDestination.MY_LISTINGS ||
        navigation.profileDestination == ProfileDestination.EDIT_LISTING
    ) {
        MyListingsDestination(navigation, listingsViewModel)
    } else if (navigation.profileDestination == ProfileDestination.PUBLIC_PROFILE) {
        PublicProfilePreviewDestination(
            profilePresentation.viewModel,
            sellerProfileViewModel,
            onListingClick = { listingId ->
                marketplaceViewModel.onListingClicked(listingId)
                navigation.openListing(listingId)
            },
            onBack = { navigation.profileDestination = ProfileDestination.SETTINGS }
        )
    } else {
        StudentProfileDestination(
            container,
            navigation,
            listingsViewModel,
            marketplaceViewModel,
            profilePresentation,
            messagesViewModel
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

@Composable
private fun StudentProfileDestination(
    container: AppContainer,
    navigation: MutableMainNavigationState,
    listingsViewModel: ListingsViewModel,
    marketplaceViewModel: MarketplaceViewModel,
    profilePresentation: ProfilePresentation,
    messagesViewModel: MessagesViewModel
) {
    val resolvedProfileViewModel = profilePresentation.viewModel
    val profileState by resolvedProfileViewModel.uiState.collectAsState()
    val hideReportedListings by profilePresentation.reportedListingVisibilityViewModel
        .shouldHideReportedListings.collectAsState()
    LaunchedEffect(profileState.profile?.id) {
        profilePresentation.reportedListingVisibilityViewModel.setAccount(
            profileState.profile?.id?.value?.let(::MarketplaceReporterId)
        )
    }
    val profileFlows = remember(container, navigation.profileDestination) {
        ProfileScreenFlows(
            editing = container.profile.createEditProfileViewModel(container.userMessenger),
            account = container.profile.createAccountViewModel(container.userMessenger),
            messenger = container.userMessenger,
            blockedUsers = container.profile.createBlockedUsersViewModel(container.listingChangedSignal)
        )
    }
    DisposableEffect(profileFlows) {
        onDispose {
            profileFlows.editing.viewModelScope.cancel()
            profileFlows.account.viewModelScope.cancel()
            profileFlows.blockedUsers.viewModelScope.cancel()
        }
    }
    ProfileScreen(
        viewModel = resolvedProfileViewModel,
        flows = profileFlows,
        destination = navigation.profileDestination,
        callbacks = profileCallbacks(
            container,
            navigation,
            listingsViewModel,
            marketplaceViewModel,
            messagesViewModel,
            profilePresentation.onSignOut
        ),
        themeViewModel = profilePresentation.themeViewModel,
        reportedListingVisibilitySetting = ReportedListingVisibilitySetting(
            hideReportedListings = hideReportedListings,
            onChanged = { shouldHide ->
                profilePresentation.reportedListingVisibilityViewModel.setHideReportedListings(shouldHide) {
                    container.listingChangedSignal.emit()
                }
            }
        )
    )
}
