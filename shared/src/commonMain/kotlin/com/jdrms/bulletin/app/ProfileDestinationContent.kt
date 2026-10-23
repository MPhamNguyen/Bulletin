package com.jdrms.bulletin.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.app.di.AppContainer
import com.jdrms.bulletin.app.di.blockedUsersViewModelFactory
import com.jdrms.bulletin.app.di.createAccountViewModel
import com.jdrms.bulletin.app.di.createEditProfileViewModel
import com.jdrms.bulletin.app.navigation.AppDestination
import com.jdrms.bulletin.app.navigation.MutableMainNavigationState
import com.jdrms.bulletin.app.navigation.ProfileDestination
import com.jdrms.bulletin.app.navigation.backFromProfile
import com.jdrms.bulletin.app.navigation.backFromProfileDestination
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.presentation.ListingsViewModel
import com.jdrms.bulletin.domain.listings.presentation.MyListingsScreen
import com.jdrms.bulletin.domain.messages.presentation.MessagesViewModel
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListing
import com.jdrms.bulletin.domain.profile.presentation.ProfileScreen
import com.jdrms.bulletin.domain.profile.presentation.ProfileScreenCallbacks
import com.jdrms.bulletin.domain.profile.presentation.ProfileScreenFlows
import kotlinx.coroutines.cancel

@Composable
internal fun ProfileDestinationContent(
    container: AppContainer,
    navigation: MutableMainNavigationState,
    listingsViewModel: ListingsViewModel,
    profilePresentation: ProfilePresentation,
    messagesViewModel: MessagesViewModel
) {
    if (navigation.profileDestination == ProfileDestination.MY_LISTINGS ||
        navigation.profileDestination == ProfileDestination.EDIT_LISTING
    ) {
        MyListingsDestination(navigation, listingsViewModel)
    } else {
        StudentProfileDestination(container, navigation, listingsViewModel, profilePresentation, messagesViewModel)
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
    profilePresentation: ProfilePresentation,
    messagesViewModel: MessagesViewModel
) {
    val resolvedProfileViewModel = profilePresentation.viewModel
    val profileFlows = remember(container, navigation.profileDestination) {
        ProfileScreenFlows(
            editing = container.createEditProfileViewModel(),
            account = container.createAccountViewModel(),
            messenger = container.userMessenger,
            blockedUsers = container.blockedUsersViewModelFactory()
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
            messagesViewModel,
            profilePresentation.onSignOut
        ),
        themeViewModel = profilePresentation.themeViewModel
    )
}

private fun profileCallbacks(
    container: AppContainer,
    navigation: MutableMainNavigationState,
    listingsViewModel: ListingsViewModel,
    messagesViewModel: MessagesViewModel,
    onSignOut: () -> Unit
): ProfileScreenCallbacks {
    val conversationActions = ListingConversationActions(container, navigation, messagesViewModel)
    return ProfileScreenCallbacks(
        onNavigate = { destination ->
            if (destination == ProfileDestination.EDIT_ACCOUNT) {
                navigation.editReturnDestination = navigation.profileDestination
            }
            navigation.profileDestination = destination
        },
        onBack = {
            navigation.profileDestination = backFromProfileDestination(
                navigation.profileDestination,
                navigation.editReturnDestination
            )
        },
        onSignOut = onSignOut,
        onMyListingsClick = {
            listingsViewModel.clearMessages()
            navigation.profileDestination = ProfileDestination.MY_LISTINGS
        },
        onMessageSeller = conversationActions::messageSeller,
        onMessageSent = {
            navigation.apply(navigation.snapshot().selectBottomNavigationDestination(AppDestination.MESSAGES))
        },
        onSeeChat = conversationActions::seeChat,
        hasExistingConversation = conversationActions::hasExistingConversation,
        onSubmitReview = profileReviewCallback(container)
    )
}

private fun profileReviewCallback(
    container: AppContainer
): suspend (ProfileBookmarkedListing, String, Int, String) -> Result<Unit> =
    { listing, reviewerId, score, comment ->
        container.submitMarketplaceReview.submitForSoldListing(
            listingId = listing.id,
            buyerId = listing.buyerId,
            sellerId = listing.sellerId,
            isSold = listing.isSold,
            reviewerId = reviewerId,
            score = score,
            comment = comment
        )
    }
