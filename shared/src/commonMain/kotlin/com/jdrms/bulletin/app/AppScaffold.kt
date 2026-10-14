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
import com.jdrms.bulletin.app.navigation.AppDestination
import com.jdrms.bulletin.app.navigation.MainNavigationState
import com.jdrms.bulletin.app.navigation.ProfileDestination
import com.jdrms.bulletin.app.navigation.backFromProfile
import com.jdrms.bulletin.app.navigation.backFromProfileDestination
import com.jdrms.bulletin.app.theme.ThemeViewModel
import com.jdrms.bulletin.core.designsystem.BulletinTheme
import com.jdrms.bulletin.domain.home.presentation.HomeScreen
import com.jdrms.bulletin.domain.listings.presentation.ListingsScreen
import com.jdrms.bulletin.domain.listings.presentation.MyListingsScreen
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceScreen
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceScreenActions
import com.jdrms.bulletin.domain.messages.presentation.MessagesScreen
import com.jdrms.bulletin.domain.profile.presentation.ProfileScreen
import com.jdrms.bulletin.domain.profile.presentation.ProfileScreenCallbacks
import com.jdrms.bulletin.domain.profile.presentation.ProfileScreenFlows
import com.jdrms.bulletin.domain.profile.presentation.ProfileViewModel
import kotlinx.coroutines.cancel

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

    BulletinTheme(darkTheme = darkTheme) {
        var navigationState by remember { mutableStateOf(MainNavigationState()) }
        var editReturnDestination by remember { mutableStateOf(ProfileDestination.PROFILE) }

        val homeViewModel = remember { container.createHomeViewModel() }
        val marketplaceViewModel = remember { container.createMarketplaceViewModel() }
        val sellerProfileViewModel = remember { container.createSellerProfileViewModel() }
        val marketplaceUserReportViewModel = remember { container.createMarketplaceUserReportViewModel() }
        val listingsViewModel = remember { container.createListingsViewModel() }
        val messagesViewModel = remember { container.createMessagesViewModel() }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            bottomBar = {
                BulletinBottomNavigationBar(
                    currentDestination = navigationState.currentDestination,
                    onDestinationSelected = { destination ->
                        if (destination == AppDestination.MESSAGES) messagesViewModel.loadConversations()
                        if (destination != AppDestination.PROFILE) {
                            listingsViewModel.cancelEditing()
                            listingsViewModel.clearMessages()
                        }
                        navigationState = navigationState.selectBottomNavigationDestination(destination)
                    }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                when (navigationState.currentDestination) {
                    AppDestination.HOME -> HomeScreen(homeViewModel)
                    AppDestination.MARKETPLACE -> MarketplaceScreen(
                        viewModel = marketplaceViewModel,
                        sellerProfileViewModel = sellerProfileViewModel,
                        userReportViewModel = marketplaceUserReportViewModel,
                        actions = MarketplaceScreenActions(
                            onMessageSeller = { listingId, sellerId, sellerName, content ->
                                val sendResult = sendListingMessage(
                                    container,
                                    navigationState,
                                    ListingMessageRequest(listingId, sellerId, sellerName, content)
                                )
                                openSentListingConversation(sendResult, messagesViewModel)
                                navigationState = sendResult.navigationState
                                sendResult.sent
                            },
                            hasExistingConversation = { listingId, sellerId ->
                                hasListingConversation(container, listingId, sellerId)
                            },
                            onSeeChat = { listingId, sellerId ->
                                openListingConversation(container, messagesViewModel, listingId, sellerId)
                                navigationState = navigationState.selectBottomNavigationDestination(
                                    AppDestination.MESSAGES
                                )
                            }
                        )
                    )
                    AppDestination.LISTINGS -> ListingsScreen(listingsViewModel)
                    AppDestination.MESSAGES -> MessagesScreen(messagesViewModel)
                    AppDestination.PROFILE -> {
                        if (navigationState.profileDestination == ProfileDestination.MY_LISTINGS ||
                            navigationState.profileDestination == ProfileDestination.EDIT_LISTING
                        ) {
                            MyListingsScreen(
                                viewModel = listingsViewModel,
                                onBack = {
                                    listingsViewModel.clearMessages()
                                    listingsViewModel.cancelEditing()
                                    navigationState = navigationState.copy(
                                        profileDestination = backFromProfile(navigationState.profileDestination)
                                    )
                                },
                                onEditListing = {
                                    navigationState = navigationState.copy(
                                        profileDestination = ProfileDestination.EDIT_LISTING
                                    )
                                }
                            )
                        } else {
                            val resolvedProfileViewModel = remember(
                                profileViewModel,
                                container,
                                navigationState.profileDestination
                            ) {
                                resolveProvidedOrCreate(profileViewModel) { container.createProfileViewModel() }
                            }
                            val profileFlows = remember(container, navigationState.profileDestination) {
                                ProfileScreenFlows(
                                    editing = container.createEditProfileViewModel(),
                                    account = container.createAccountViewModel(),
                                    messenger = container.userMessenger
                                )
                            }
                            DisposableEffect(profileFlows, resolvedProfileViewModel) {
                                onDispose {
                                    profileFlows.editing.viewModelScope.cancel()
                                    profileFlows.account.viewModelScope.cancel()
                                    if (profileViewModel == null) resolvedProfileViewModel.viewModelScope.cancel()
                                }
                            }
                            ProfileScreen(
                                viewModel = resolvedProfileViewModel,
                                flows = profileFlows,
                                destination = navigationState.profileDestination,
                                callbacks = ProfileScreenCallbacks(
                                    onNavigate = { destination ->
                                        if (destination == ProfileDestination.EDIT_ACCOUNT) {
                                            editReturnDestination = navigationState.profileDestination
                                        }
                                        navigationState = navigationState.copy(profileDestination = destination)
                                    },
                                    onBack = {
                                        navigationState = navigationState.copy(
                                            profileDestination = backFromProfileDestination(
                                                navigationState.profileDestination,
                                                editReturnDestination
                                            )
                                        )
                                    },
                                    onSignOut = onSignOut,
                                    onMyListingsClick = {
                                        listingsViewModel.clearMessages()
                                        navigationState = navigationState.copy(
                                            profileDestination = ProfileDestination.MY_LISTINGS
                                        )
                                    },
                                    onCreateListingClick = {
                                        listingsViewModel.clearMessages()
                                        navigationState = navigationState.copy(
                                            currentDestination = AppDestination.LISTINGS
                                        )
                                    },
                                    onMessageSeller = { listingId, sellerId, sellerName, content ->
                                        val sendResult = sendListingMessage(
                                            container,
                                            navigationState,
                                            ListingMessageRequest(listingId, sellerId, sellerName, content)
                                        )
                                        openSentListingConversation(sendResult, messagesViewModel)
                                        navigationState = sendResult.navigationState
                                        sendResult.sent
                                    },
                                    onMessageSent = {
                                        navigationState = navigationState.selectBottomNavigationDestination(
                                            AppDestination.MESSAGES
                                        )
                                    },
                                    onSeeChat = { listingId, sellerId ->
                                        openListingConversation(container, messagesViewModel, listingId, sellerId)
                                        navigationState = navigationState.selectBottomNavigationDestination(
                                            AppDestination.MESSAGES
                                        )
                                    },
                                    hasExistingConversation = { listingId, sellerId ->
                                        hasListingConversation(container, listingId, sellerId)
                                    }
                                ),
                                themeViewModel = themeViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun <T> resolveProvidedOrCreate(provided: T?, create: () -> T): T = provided ?: create()
