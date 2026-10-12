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
    val messenger: UserMessenger
)

data class ProfileScreenCallbacks(
    val onNavigate: (ProfileDestination) -> Unit,
    val onBack: () -> Unit,
    val onSignOut: () -> Unit,
    val onMyListingsClick: () -> Unit,
    val onCreateListingClick: () -> Unit,
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
    themeViewModel: ThemeViewModel? = null
) {
    val profileState by viewModel.uiState.collectAsState()
    val editState by flows.editing.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.refreshActiveListings() }
    LaunchedEffect(destination) {
        if (destination == ProfileDestination.BOOKMARKED_LISTINGS) {
            viewModel.loadBookmarkedListings()
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
            themeViewModel
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
    themeViewModel: ThemeViewModel?
) {
    when (destination) {
        ProfileDestination.PROFILE -> ProfileLandingDestination(states.profile, flows.editing, callbacks)
        ProfileDestination.EDIT_ACCOUNT -> ProfileEditDestination(states.edit, flows, callbacks)
        ProfileDestination.SETTINGS -> ProfileSettingsDestination(states.profile, flows, callbacks, themeViewModel)
        ProfileDestination.BOOKMARKED_LISTINGS -> {
            BookmarkedListingsView(
                state = BookmarkedListingsViewState(
                    listings = states.profile.bookmarkedListings,
                    isLoading = states.profile.isLoadingBookmarkedListings,
                    isLoadingMore = states.profile.isLoadingMoreBookmarkedListings,
                    hasMore = states.profile.bookmarkedListingsNextCursor != null,
                    errorMessage = states.profile.errorMessage,
                    removingListingId = states.profile.removingBookmarkedListingId
                ),
                actions = BookmarkedListingsActions(
                    onListingClick = states.viewModel::viewBookmarkedListing,
                    onRemoveBookmark = states.viewModel::removeBookmarkedListing,
                    onRefresh = states.viewModel::loadBookmarkedListings,
                    onLoadMore = states.viewModel::loadMoreBookmarkedListings,
                    onBack = callbacks.onBack
                )
            )
            states.profile.selectedBookmarkedListing?.let { listing ->
                BookmarkedListingDetailSheet(
                    listing = listing,
                    isRemovingBookmark = states.profile.removingBookmarkedListingId == listing.id,
                    errorMessage = states.profile.errorMessage,
                    actions = BookmarkedListingDetailActions(
                        onDismiss = states.viewModel::dismissBookmarkedListing,
                        onRemoveBookmark = { states.viewModel.removeBookmarkedListing(listing.id) },
                        canMessageSeller = states.profile.profile?.id?.value != listing.sellerId,
                        onMessageSeller = { content ->
                            callbacks.onMessageSeller(listing.id, listing.sellerId, listing.sellerName, content)
                        },
                        hasExistingConversation = {
                            callbacks.hasExistingConversation(listing.id, listing.sellerId)
                        },
                        onMessageSent = callbacks.onMessageSent,
                        onSeeChat = callbacks.onSeeChat
                    )
                )
            }
        }
        ProfileDestination.NOTIFICATIONS -> NotificationsView(callbacks.onBack)
        ProfileDestination.PRIVACY -> PrivacyView(callbacks.onBack)
        ProfileDestination.HELP_AND_SUPPORT -> HelpAndSupportView(callbacks.onBack)
        ProfileDestination.TERMS_AND_CONDITIONS -> TermsAndConditionsView(callbacks.onBack)
        ProfileDestination.PUBLIC_PROFILE -> PublicProfileView(states.profile.profile, callbacks.onBack)
        ProfileDestination.MY_LISTINGS, ProfileDestination.EDIT_LISTING -> Unit
    }
}

@Composable
private fun ProfileLandingDestination(
    state: ProfileUiState,
    editing: EditProfileViewModel,
    callbacks: ProfileScreenCallbacks
) {
    val photoPicker = rememberProfilePhotoPicker(
        onPhotoSelected = editing::uploadPhoto,
        onError = editing::onPhotoSelectionError
    )
    MarketplaceProfileView(
        uiState = state,
        onSettingsClick = { callbacks.onNavigate(ProfileDestination.SETTINGS) },
        onEditProfileClick = { callbacks.onNavigate(ProfileDestination.EDIT_ACCOUNT) },
        onChangePhotoClick = photoPicker,
        onMyListingsClick = callbacks.onMyListingsClick,
        onBookmarkedListingsClick = { callbacks.onNavigate(ProfileDestination.BOOKMARKED_LISTINGS) },
        onCreateListingClick = callbacks.onCreateListingClick
    )
}

@Composable
private fun ProfileEditDestination(
    state: EditProfileUiState,
    flows: ProfileScreenFlows,
    callbacks: ProfileScreenCallbacks
) {
    val photoPicker = rememberProfilePhotoPicker(
        onPhotoSelected = flows.editing::uploadPhoto,
        onError = flows.editing::onPhotoSelectionError
    )
    EditProfileView(
        uiState = state,
        actions = EditProfileActions(
            onCancel = callbacks.onBack,
            onDraftChanged = flows.editing::onDraftChanged,
            onUpdate = { flows.editing.save { callbacks.onNavigate(ProfileDestination.PROFILE) } },
            onChangePhoto = photoPicker,
            onSignOut = { flows.account.signOut(callbacks.onSignOut) }
        )
    )
}

@Composable
private fun ProfileSettingsDestination(
    state: ProfileUiState,
    flows: ProfileScreenFlows,
    callbacks: ProfileScreenCallbacks,
    themeViewModel: ThemeViewModel?
) {
    SettingsView(
        uiState = state,
        onBack = callbacks.onBack,
        actions = SettingsActions(
            onEditAccount = { callbacks.onNavigate(ProfileDestination.EDIT_ACCOUNT) },
            onViewPublicProfile = { callbacks.onNavigate(ProfileDestination.PUBLIC_PROFILE) },
            onNotifications = { callbacks.onNavigate(ProfileDestination.NOTIFICATIONS) },
            onPrivacy = { callbacks.onNavigate(ProfileDestination.PRIVACY) },
            onHelpSupport = { callbacks.onNavigate(ProfileDestination.HELP_AND_SUPPORT) },
            onTermsConditions = { callbacks.onNavigate(ProfileDestination.TERMS_AND_CONDITIONS) },
            onSignOut = { flows.account.signOut(callbacks.onSignOut) },
            onConfirmDeleteProfile = { flows.account.deleteProfile(callbacks.onSignOut) }
        ),
        themePreference = themeViewModel?.themePreference?.collectAsState()?.value,
        onThemePreferenceChanged = themeViewModel?.let { it::setThemePreference }
    )
}
