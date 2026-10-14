package com.jdrms.bulletin.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.app.di.AppContainer
import com.jdrms.bulletin.app.di.blockedUsersViewModelFactory
import com.jdrms.bulletin.app.di.createAccountViewModel
import com.jdrms.bulletin.app.di.createEditProfileViewModel
import com.jdrms.bulletin.app.di.createHomeViewModel
import com.jdrms.bulletin.app.di.createListingsViewModel
import com.jdrms.bulletin.app.di.createMarketplaceUserReportViewModel
import com.jdrms.bulletin.app.di.createMarketplaceViewModel
import com.jdrms.bulletin.app.di.createMessagesViewModel
import com.jdrms.bulletin.app.di.createProfileViewModel
import com.jdrms.bulletin.app.di.createSellerProfileViewModel
import com.jdrms.bulletin.app.di.marketplaceUserBlockViewModelFactory
import com.jdrms.bulletin.app.navigation.AppDestination
import com.jdrms.bulletin.app.navigation.ProfileDestination
import com.jdrms.bulletin.app.navigation.backFromProfile
import com.jdrms.bulletin.app.navigation.backFromProfileDestination
import com.jdrms.bulletin.app.theme.ThemeViewModel
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.designsystem.BulletinTheme
import com.jdrms.bulletin.domain.home.presentation.HomeScreen
import com.jdrms.bulletin.domain.home.presentation.HomeViewModel
import com.jdrms.bulletin.domain.listings.presentation.ListingsScreen
import com.jdrms.bulletin.domain.listings.presentation.ListingsViewModel
import com.jdrms.bulletin.domain.listings.presentation.MyListingsScreen
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceScreen
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceScreenActions
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceUserBlockViewModel
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceUserReportViewModel
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceViewModel
import com.jdrms.bulletin.domain.marketplace.presentation.SellerProfileViewModel
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.presentation.MessagesScreen
import com.jdrms.bulletin.domain.messages.presentation.MessagesViewModel
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListing
import com.jdrms.bulletin.domain.profile.presentation.ProfileScreen
import com.jdrms.bulletin.domain.profile.presentation.ProfileScreenCallbacks
import com.jdrms.bulletin.domain.profile.presentation.ProfileScreenFlows
import com.jdrms.bulletin.domain.profile.presentation.ProfileViewModel
import kotlinx.coroutines.cancel

internal class MainNavigationState {
    var currentDestination by mutableStateOf(AppDestination.HOME)
    var profileDestination by mutableStateOf(ProfileDestination.PROFILE)
    var editReturnDestination by mutableStateOf(ProfileDestination.PROFILE)
    var pendingConversationId by mutableStateOf<ConversationId?>(null)

    fun snapshot() = com.jdrms.bulletin.app.navigation.MainNavigationState(currentDestination, profileDestination)

    fun apply(snapshot: com.jdrms.bulletin.app.navigation.MainNavigationState) {
        currentDestination = snapshot.currentDestination
        profileDestination = snapshot.profileDestination
    }
}

private class ProfilePresentation(
    val providedViewModel: ProfileViewModel?,
    val themeViewModel: ThemeViewModel?,
    val onSignOut: () -> Unit
)

private class MainScaffoldModels(
    val home: HomeViewModel,
    val marketplace: MarketplaceViewModel,
    val sellerProfile: SellerProfileViewModel,
    val marketplaceUserReport: MarketplaceUserReportViewModel,
    val marketplaceUserBlock: MarketplaceUserBlockViewModel,
    val listings: ListingsViewModel,
    val messages: MessagesViewModel
)

@Composable
fun MainAppScaffold(
    appContainer: AppContainer? = null,
    profileViewModel: ProfileViewModel? = null,
    themeViewModel: ThemeViewModel? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    onSignOut: () -> Unit = {}
) {
    val isInspectionMode = LocalInspectionMode.current
    val container = appContainer ?: remember { AppContainer(isInspectionMode = isInspectionMode) }
    val navigation = remember { MainNavigationState() }
    val models = remember(container) {
        MainScaffoldModels(
            container.createHomeViewModel(),
            container.createMarketplaceViewModel(),
            container.createSellerProfileViewModel(),
            container.createMarketplaceUserReportViewModel(),
            container.marketplaceUserBlockViewModelFactory(),
            container.createListingsViewModel(),
            container.createMessagesViewModel()
        )
    }
    val profilePresentation = ProfilePresentation(profileViewModel, themeViewModel, onSignOut)

    BulletinTheme(darkTheme = darkTheme) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            bottomBar = {
                BulletinBottomNavigationBar(
                    currentDestination = navigation.currentDestination,
                    onDestinationSelected = { destination -> navigateTo(destination, navigation, models.listings) }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                MainAppContent(container, navigation, models, profilePresentation)
            }
        }
    }
}

private fun navigateTo(destination: AppDestination, navigation: MainNavigationState, listings: ListingsViewModel) {
    if (destination != AppDestination.PROFILE || navigation.profileDestination != ProfileDestination.PROFILE) {
        listings.cancelEditing()
        listings.clearMessages()
    }
    navigation.profileDestination = ProfileDestination.PROFILE
    navigation.currentDestination = destination
}

@Composable
private fun MainAppContent(
    container: AppContainer,
    navigation: MainNavigationState,
    models: MainScaffoldModels,
    profilePresentation: ProfilePresentation
) {
    when (navigation.currentDestination) {
        AppDestination.HOME -> HomeScreen(models.home)
        AppDestination.MARKETPLACE -> MarketplaceScreen(
            viewModel = models.marketplace,
            sellerProfileViewModel = models.sellerProfile,
            userReportViewModel = models.marketplaceUserReport,
            userBlockViewModel = models.marketplaceUserBlock,
            actions = MarketplaceScreenActions(
                onMessageSeller = { listingId, sellerId, sellerName, content ->
                    val result = sendListingMessage(
                        container,
                        navigation.snapshot(),
                        ListingMessageRequest(listingId, sellerId, sellerName, content)
                    )
                    openSentListingConversation(result, models.messages)
                    navigation.apply(result.navigationState)
                    result.sent
                },
                hasExistingConversation = { listingId, sellerId ->
                    hasListingConversation(container, listingId, sellerId)
                },
                onSeeChat = { listingId, sellerId ->
                    openListingConversation(container, models.messages, listingId, sellerId)
                    navigation.apply(
                        navigation.snapshot().selectBottomNavigationDestination(AppDestination.MESSAGES)
                    )
                }
            )
        )
        AppDestination.LISTINGS -> ListingsScreen(models.listings)
        AppDestination.MESSAGES -> MessagesScreen(models.messages)
        AppDestination.PROFILE -> ProfileDestinationContent(
            container,
            navigation,
            models.listings,
            profilePresentation,
            models.messages
        )
    }
}

@Composable
private fun ProfileDestinationContent(
    container: AppContainer,
    navigation: MainNavigationState,
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
private fun MyListingsDestination(navigation: MainNavigationState, listingsViewModel: ListingsViewModel) {
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
    navigation: MainNavigationState,
    listingsViewModel: ListingsViewModel,
    profilePresentation: ProfilePresentation,
    messagesViewModel: MessagesViewModel
) {
    val provided = profilePresentation.providedViewModel
    val destination = navigation.profileDestination
    val resolvedProfileViewModel = remember(provided, container, destination) {
        resolveProvidedOrCreate(provided) { container.createProfileViewModel() }
    }
    val profileFlows = remember(container, navigation.profileDestination) {
        ProfileScreenFlows(
            editing = container.createEditProfileViewModel(),
            account = container.createAccountViewModel(),
            messenger = container.userMessenger,
            blockedUsers = container.blockedUsersViewModelFactory()
        )
    }
    DisposableEffect(profileFlows, resolvedProfileViewModel) {
        onDispose {
            profileFlows.editing.viewModelScope.cancel()
            profileFlows.account.viewModelScope.cancel()
            profileFlows.blockedUsers.viewModelScope.cancel()
            if (profilePresentation.providedViewModel == null) resolvedProfileViewModel.viewModelScope.cancel()
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
    navigation: MainNavigationState,
    listingsViewModel: ListingsViewModel,
    messagesViewModel: MessagesViewModel,
    onSignOut: () -> Unit
) = ProfileScreenCallbacks(
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
    onCreateListingClick = {
        listingsViewModel.clearMessages()
        navigation.currentDestination = AppDestination.LISTINGS
    },
    onMessageSeller = { listingId, sellerId, sellerName, content ->
        val result = sendListingMessage(
            container,
            navigation.snapshot(),
            ListingMessageRequest(listingId, sellerId, sellerName, content)
        )
        openSentListingConversation(result, messagesViewModel)
        navigation.apply(result.navigationState)
        result.sent
    },
    onMessageSent = { navigation.openMessages() },
    onSeeChat = profileSeeChatCallback(container, navigation, messagesViewModel),
    hasExistingConversation = profileConversationChecker(container),
    onSubmitReview = profileReviewCallback(container)
)

private fun profileSeeChatCallback(
    container: AppContainer,
    navigation: MainNavigationState,
    messagesViewModel: MessagesViewModel
): suspend (String, String) -> Unit = { listingId, sellerId ->
    openListingConversation(container, messagesViewModel, listingId, sellerId)
    navigation.openMessages()
}

private fun profileConversationChecker(
    container: AppContainer
): suspend (String, String) -> Boolean = { listingId, sellerId ->
    hasListingConversation(container, listingId, sellerId)
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

private fun MainNavigationState.openMessages() {
    apply(snapshot().selectBottomNavigationDestination(AppDestination.MESSAGES))
}

internal fun <T> resolveProvidedOrCreate(provided: T?, create: () -> T): T = provided ?: create()
