package com.jdrms.bulletin.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.app.di.AppContainer
import com.jdrms.bulletin.app.di.blockedUsersViewModelFactory
import com.jdrms.bulletin.app.di.createAccountViewModel
import com.jdrms.bulletin.app.di.createEditProfileViewModel
import com.jdrms.bulletin.app.di.createHomeViewModel
import com.jdrms.bulletin.app.di.createListingReportViewModel
import com.jdrms.bulletin.app.di.createListingsViewModel
import com.jdrms.bulletin.app.di.createMarketplaceUserReportViewModel
import com.jdrms.bulletin.app.di.createMarketplaceViewModel
import com.jdrms.bulletin.app.di.createMessagesViewModel
import com.jdrms.bulletin.app.di.createProfileViewModel
import com.jdrms.bulletin.app.di.createSellerProfileViewModel
import com.jdrms.bulletin.app.di.marketplaceUserBlockViewModelFactory
import com.jdrms.bulletin.app.navigation.AppDestination
import com.jdrms.bulletin.app.navigation.MutableMainNavigationState
import com.jdrms.bulletin.app.navigation.ProfileDestination
import com.jdrms.bulletin.app.navigation.backFromProfile
import com.jdrms.bulletin.app.navigation.backFromProfileDestination
import com.jdrms.bulletin.app.theme.ThemeViewModel
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.designsystem.BulletinTheme
import com.jdrms.bulletin.core.designsystem.LocalNavbarHeight
import com.jdrms.bulletin.core.designsystem.platformReducedMotion
import com.jdrms.bulletin.domain.home.presentation.HomeScreen
import com.jdrms.bulletin.domain.home.presentation.HomeViewModel
import com.jdrms.bulletin.domain.listings.presentation.ListingsScreen
import com.jdrms.bulletin.domain.listings.presentation.ListingsViewModel
import com.jdrms.bulletin.domain.listings.presentation.MyListingsScreen
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.presentation.ListingReportViewModel
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceScreen
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceScreenActions
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceUserBlockViewModel
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceUserReportViewModel
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceViewModel
import com.jdrms.bulletin.domain.marketplace.presentation.SellerProfileViewModel
import com.jdrms.bulletin.domain.messages.presentation.MessagesScreen
import com.jdrms.bulletin.domain.messages.presentation.MessagesViewModel
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListing
import com.jdrms.bulletin.domain.profile.presentation.ProfileScreen
import com.jdrms.bulletin.domain.profile.presentation.ProfileScreenCallbacks
import com.jdrms.bulletin.domain.profile.presentation.ProfileScreenFlows
import com.jdrms.bulletin.domain.profile.presentation.ProfileViewModel
import kotlinx.coroutines.cancel

private class ProfilePresentation(
    val viewModel: ProfileViewModel,
    val themeViewModel: ThemeViewModel?,
    val onSignOut: () -> Unit
)

private class MainScaffoldModels(
    val home: HomeViewModel,
    val marketplace: MarketplaceViewModel,
    val sellerProfile: SellerProfileViewModel,
    val marketplaceUserReport: MarketplaceUserReportViewModel,
    val listingReport: ListingReportViewModel,
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
    val navigation = remember { MutableMainNavigationState() }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val navigationBarInset = with(density) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }
    val navbarScrollBehavior = remember(density) {
        NavbarScrollBehavior(with(density) { NAVBAR_SCROLL_THRESHOLD_DP.dp.toPx() })
    }
    val navbarNestedScrollConnection = remember(navbarScrollBehavior) {
        NavbarNestedScrollConnection(navbarScrollBehavior)
    }
    var navbarHeight by remember { mutableStateOf(0.dp) }
    var profileTabCenter by remember { mutableStateOf(Offset.Zero) }
    var profileSaveTrigger by remember { mutableIntStateOf(0) }
    val models = remember(container) {
        MainScaffoldModels(
            container.createHomeViewModel(),
            container.createMarketplaceViewModel(),
            container.createSellerProfileViewModel(),
            container.createMarketplaceUserReportViewModel(),
            container.createListingReportViewModel(),
            container.marketplaceUserBlockViewModelFactory(),
            container.createListingsViewModel(),
            container.createMessagesViewModel()
        )
    }
    // Keep the profile read model alive for the authenticated app session so its summary is
    // fetched alongside the other app models, before the user opens the Profile destination.
    val resolvedProfileViewModel = remember(profileViewModel, container) {
        resolveProvidedOrCreate(profileViewModel) { container.createProfileViewModel() }
    }
    DisposableEffect(resolvedProfileViewModel, profileViewModel) {
        onDispose {
            if (profileViewModel == null) resolvedProfileViewModel.viewModelScope.cancel()
        }
    }
    val profilePresentation = ProfilePresentation(resolvedProfileViewModel, themeViewModel, onSignOut)

    LaunchedEffect(navigation.currentDestination) {
        navbarScrollBehavior.onDestinationChanged()
    }
    LaunchedEffect(navigation.currentDestination, navigation.profileDestination) {
        if (navigation.currentDestination == AppDestination.PROFILE &&
            navigation.profileDestination in setOf(ProfileDestination.PROFILE, ProfileDestination.BOOKMARKED_LISTINGS)
        ) {
            resolvedProfileViewModel.loadBookmarkedListings()
        }
    }

    BulletinTheme(darkTheme = darkTheme) {
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground
            ) { paddingValues ->
                val contentModifier = Modifier
                    .padding(paddingValues)
                    .then(
                        if (navigation.currentDestination.hidesOnScroll || !navigation.showsBottomNavigation) {
                            Modifier
                        } else {
                            Modifier.padding(bottom = navbarHeight)
                        }
                    )
                    .fillMaxSize()
                CompositionLocalProvider(LocalNavbarHeight provides navbarHeight) {
                    Box(
                        modifier = if (navigation.currentDestination.hidesOnScroll) {
                            contentModifier.nestedScroll(navbarNestedScrollConnection)
                        } else {
                            contentModifier
                        }
                    ) {
                        MainAppContent(
                            container,
                            navigation,
                            models,
                            profilePresentation,
                            profileTabCenter,
                            onListingSaved = { profileSaveTrigger++ },
                            onContentNoLongerScrollable = navbarScrollBehavior::reveal
                        )
                    }
                }
            }
            Navbar(
                currentDestination = navigation.currentDestination,
                isVisible = {
                    navigation.showsBottomNavigation && navbarScrollBehavior.isVisible
                },
                onDestinationSelected = { destination -> navigateTo(destination, navigation, models.listings) },
                onFocused = navbarScrollBehavior::reveal,
                onDestinationPositioned = { destination, center ->
                    if (destination == AppDestination.PROFILE) profileTabCenter = center
                },
                profileSaveTrigger = profileSaveTrigger,
                reducedMotion = platformReducedMotion(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(100f)
                    .onSizeChanged {
                        navbarHeight = with(density) { it.height.toDp() } - navigationBarInset
                    }
            )
        }
    }
}

private const val NAVBAR_SCROLL_THRESHOLD_DP = 32

private fun navigateTo(
    destination: AppDestination,
    navigation: MutableMainNavigationState,
    listings: ListingsViewModel
) {
    if (destination != AppDestination.PROFILE || navigation.profileDestination != ProfileDestination.PROFILE) {
        listings.cancelEditing()
        listings.clearMessages()
    }
    navigation.apply(navigation.snapshot().selectBottomNavigationDestination(destination))
}

@Composable
private fun MainAppContent(
    container: AppContainer,
    navigation: MutableMainNavigationState,
    models: MainScaffoldModels,
    profilePresentation: ProfilePresentation,
    profileTabCenter: Offset,
    onListingSaved: () -> Unit,
    onContentNoLongerScrollable: () -> Unit
) {
    when (navigation.currentDestination) {
        AppDestination.HOME -> {
            val conversationActions = ListingConversationActions(container, navigation, models.messages)
            val marketplaceState by models.marketplace.uiState.collectAsState()
            var failedSaveItem by remember { mutableStateOf<MarketplaceItemId?>(null) }
            HomeScreen(
                viewModel = models.home,
                onSendQuickMessage = { item, content ->
                    val sellerId = item.sellerId
                    val sellerName = item.sellerName
                    sellerId != null && sellerName != null && conversationActions.sendQuickMessage(
                        item.id.value,
                        sellerId,
                        sellerName,
                        content
                    )
                },
                onSaveListing = { item ->
                    onListingSaved()
                    val itemId = MarketplaceItemId(item.id.value)
                    failedSaveItem = itemId
                    models.marketplace.bookmarkListing(itemId)
                },
                saveErrorMessage = marketplaceState.bookmarkErrorMessage,
                onRetrySave = { failedSaveItem?.let { models.marketplace.bookmarkListing(it) } },
                profileTargetPosition = profileTabCenter
            )
        }
        AppDestination.MARKETPLACE -> {
            val conversationActions = ListingConversationActions(container, navigation, models.messages)
            MarketplaceScreen(
                viewModel = models.marketplace,
                sellerProfileViewModel = models.sellerProfile,
                userReportViewModel = models.marketplaceUserReport,
                listingReportViewModel = models.listingReport,
                userBlockViewModel = models.marketplaceUserBlock,
                onContentNoLongerScrollable = onContentNoLongerScrollable,
                actions = MarketplaceScreenActions(
                    onMessageSeller = conversationActions::messageSeller,
                    hasExistingConversation = conversationActions::hasExistingConversation,
                    onSeeChat = conversationActions::seeChat
                )
            )
        }
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

internal fun <T> resolveProvidedOrCreate(provided: T?, create: () -> T): T = provided ?: create()
