package com.jdrms.bulletin.app

import com.jdrms.bulletin.app.di.AppContainer
import com.jdrms.bulletin.app.navigation.AppDestination
import com.jdrms.bulletin.app.navigation.MutableMainNavigationState
import com.jdrms.bulletin.app.navigation.ProfileDestination
import com.jdrms.bulletin.app.navigation.backFromProfileDestination
import com.jdrms.bulletin.domain.listings.presentation.ListingsViewModel
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceViewModel
import com.jdrms.bulletin.domain.messages.presentation.MessagesViewModel
import com.jdrms.bulletin.domain.profile.presentation.ProfileScreenCallbacks

internal fun profileCallbacks(
    container: AppContainer,
    navigation: MutableMainNavigationState,
    listingsViewModel: ListingsViewModel,
    marketplaceViewModel: MarketplaceViewModel,
    messagesViewModel: MessagesViewModel,
    onSignOut: () -> Unit
): ProfileScreenCallbacks {
    val conversationActions = ListingConversationActions(container.messages, navigation, messagesViewModel)
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
        onOpenListing = { listingId ->
            marketplaceViewModel.onListingClicked(listingId)
            navigation.openListing(listingId)
        },
        onMessageSeller = conversationActions::messageSeller,
        onMessageSent = {
            navigation.apply(navigation.snapshot().selectBottomNavigationDestination(AppDestination.MESSAGES))
        },
        onSeeChat = conversationActions::seeChat,
        hasExistingConversation = conversationActions::hasExistingConversation
    )
}
