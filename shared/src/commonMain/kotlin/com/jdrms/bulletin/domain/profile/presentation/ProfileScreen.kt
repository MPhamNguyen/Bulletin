package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.app.navigation.ProfileDestination
import com.jdrms.bulletin.app.theme.ThemeViewModel
import com.jdrms.bulletin.core.common.UserMessenger

data class ProfileScreenFlows(
    val editing: EditProfileViewModel,
    val account: AccountViewModel,
    val messenger: UserMessenger,
    val blockedUsers: BlockedUsersViewModel
)

data class ProfileScreenCallbacks(
    val onNavigate: (ProfileDestination) -> Unit,
    val onBack: () -> Unit,
    val onSignOut: () -> Unit,
    val onMyListingsClick: () -> Unit,
    val onOpenListing: (String) -> Unit = {},
    val onMessageSeller: suspend (listingId: String, sellerId: String, sellerName: String, content: String) -> Boolean =
        { _, _, _, _ -> false },
    val hasExistingConversation: suspend (listingId: String, sellerId: String) -> Boolean =
        { _, _ -> false },
    val onMessageSent: () -> Unit = {},
    val onSeeChat: suspend (listingId: String, sellerId: String) -> Unit = { _, _ -> }
)

private data class ProfileDestinationStates(
    val profile: ProfileUiState,
    val edit: EditProfileUiState,
    val viewModel: ProfileViewModel
)

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    flows: ProfileScreenFlows,
    destination: ProfileDestination,
    callbacks: ProfileScreenCallbacks,
    themeViewModel: ThemeViewModel? = null,
    reportedListingVisibilitySetting: ReportedListingVisibilitySetting? = null
) {
    val profileState by viewModel.uiState.collectAsState()
    val editState by flows.editing.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(destination) {
        if (destination == ProfileDestination.BOOKMARKED_LISTINGS) {
            viewModel.loadBookmarkedListings()
        }
        if (destination == ProfileDestination.BLOCKED_USERS) {
            flows.blockedUsers.load()
        }
    }
    LaunchedEffect(flows.messenger) {
        flows.messenger.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ProfileDestinationContent(
            destination,
            ProfileDestinationStates(profileState, editState, viewModel),
            flows,
            callbacks,
            themeViewModel,
            reportedListingVisibilitySetting
        )
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun ProfileDestinationContent(
    destination: ProfileDestination,
    states: ProfileDestinationStates,
    flows: ProfileScreenFlows,
    callbacks: ProfileScreenCallbacks,
    themeViewModel: ThemeViewModel?,
    reportedListingVisibilitySetting: ReportedListingVisibilitySetting?
) {
    when (destination) {
        ProfileDestination.PROFILE -> ProfileLandingDestination(states.profile, callbacks) {
            states.viewModel.refreshProfile()
        }
        ProfileDestination.EDIT_ACCOUNT -> ProfileEditDestination(states.edit, flows, callbacks)
        ProfileDestination.SETTINGS -> ProfileSettingsDestination(
            states.profile,
            flows,
            callbacks,
            themeViewModel,
            reportedListingVisibilitySetting
        )
        ProfileDestination.BOOKMARKED_LISTINGS -> {
            BookmarkedListingsView(
                state = BookmarkedListingsViewState(
                    listings = states.profile.filteredBookmarkedListings,
                    allListings = states.profile.bookmarkedListings,
                    filters = states.profile.bookmarkedListingFilters,
                    filterOptions = states.profile.bookmarkedListingFilterOptions,
                    isLoading = states.profile.isLoadingBookmarkedListings,
                    isLoadingMore = states.profile.isLoadingMoreBookmarkedListings,
                    hasMore = states.profile.bookmarkedListingsNextCursor != null,
                    errorMessage = states.profile.errorMessage,
                    removingListingId = states.profile.removingBookmarkedListingId
                ),
                actions = BookmarkedListingsActions(
                    onListingClick = callbacks.onOpenListing,
                    onRemoveBookmark = states.viewModel::removeBookmarkedListing,
                    onRefresh = states.viewModel::loadBookmarkedListings,
                    onLoadMore = states.viewModel::loadMoreBookmarkedListings,
                    onQueryChanged = states.viewModel::onBookmarkedQueryChanged,
                    onFiltersApplied = states.viewModel::onBookmarkedFiltersApplied,
                    onBack = callbacks.onBack
                )
            )
        }
        ProfileDestination.BLOCKED_USERS -> BlockedUsersView(
            state = flows.blockedUsers.uiState.collectAsState().value,
            onUnblock = flows.blockedUsers::unblock,
            onRefresh = flows.blockedUsers::load,
            onBack = callbacks.onBack
        )
        ProfileDestination.NOTIFICATIONS -> NotificationsView(callbacks.onBack)
        ProfileDestination.PUBLIC_PROFILE -> Unit
        ProfileDestination.MY_LISTINGS, ProfileDestination.EDIT_LISTING -> Unit
    }
}

@Composable
private fun ProfileLandingDestination(
    state: ProfileUiState,
    callbacks: ProfileScreenCallbacks,
    onRefresh: () -> Unit
) {
    MarketplaceProfileView(
        uiState = state,
        onRefresh = onRefresh,
        onSettingsClick = { callbacks.onNavigate(ProfileDestination.SETTINGS) },
        onMyListingsClick = callbacks.onMyListingsClick,
        onBookmarkedListingsClick = { callbacks.onNavigate(ProfileDestination.BOOKMARKED_LISTINGS) }
    )
}

@Composable
private fun ProfileSettingsDestination(
    state: ProfileUiState,
    flows: ProfileScreenFlows,
    callbacks: ProfileScreenCallbacks,
    themeViewModel: ThemeViewModel?,
    reportedListingVisibilitySetting: ReportedListingVisibilitySetting?
) {
    SettingsView(
        uiState = state,
        onBack = callbacks.onBack,
        actions = SettingsActions(
            onEditAccount = { callbacks.onNavigate(ProfileDestination.EDIT_ACCOUNT) },
            onViewPublicProfile = { callbacks.onNavigate(ProfileDestination.PUBLIC_PROFILE) },
            onNotifications = { callbacks.onNavigate(ProfileDestination.NOTIFICATIONS) },
            onBlockedUsers = { callbacks.onNavigate(ProfileDestination.BLOCKED_USERS) },
            onSignOut = { flows.account.signOut(callbacks.onSignOut) },
            onConfirmDeleteProfile = { flows.account.deleteProfile(callbacks.onSignOut) }
        ),
        themePreference = themeViewModel?.themePreference?.collectAsState()?.value,
        onThemePreferenceChanged = themeViewModel?.let { it::setThemePreference },
        reportedListingVisibilitySetting = reportedListingVisibilitySetting
    )
}
