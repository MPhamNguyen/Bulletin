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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.app.navigation.ProfileDestination
import com.jdrms.bulletin.app.theme.ThemeViewModel
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.common.UserMessenger
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListing
import kotlinx.coroutines.launch

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
    val onMessageSeller: suspend (listingId: String, sellerId: String, sellerName: String, content: String) -> Boolean =
        { _, _, _, _ -> false },
    val hasExistingConversation: suspend (listingId: String, sellerId: String) -> Boolean =
        { _, _ -> false },
    val onMessageSent: () -> Unit = {},
    val onSeeChat: suspend (listingId: String, sellerId: String) -> Unit = { _, _ -> },
    val onSubmitReview: suspend (
        listing: ProfileBookmarkedListing,
        reviewerId: String,
        score: Int,
        comment: String
    ) -> Result<Unit> = { _, _, _, _ -> Result.Error(UnsupportedOperationException("Review submission unavailable.")) }
)

private data class ProfileDestinationStates(
    val profile: ProfileUiState,
    val edit: EditProfileUiState,
    val viewModel: ProfileViewModel
)

private data class BookmarkedListingReviewFlow(
    val selectedListing: ProfileBookmarkedListing?,
    val isSubmitting: Boolean,
    val errorMessage: String?,
    val onOpen: (ProfileBookmarkedListing) -> Unit,
    val onDismiss: () -> Unit,
    val onSubmit: (Int, String) -> Unit
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
    var reviewListing by remember { mutableStateOf<ProfileBookmarkedListing?>(null) }
    var reviewError by remember { mutableStateOf<String?>(null) }
    var isSubmittingReview by remember { mutableStateOf(false) }
    val reviewScope = rememberCoroutineScope()

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

    val submitReview: (Int, String) -> Unit = { score, comment ->
        reviewListing?.let { listing ->
            profileState.profile?.id?.value?.let { reviewerId ->
                reviewScope.launch {
                    isSubmittingReview = true
                    reviewError = null
                    when (val result = callbacks.onSubmitReview(listing, reviewerId, score, comment)) {
                        is Result.Success -> {
                            isSubmittingReview = false
                            reviewListing = null
                        }
                        is Result.Error -> {
                            isSubmittingReview = false
                            reviewError = result.exception.message
                        }
                    }
                }
            }
        }
    }
    val reviewFlow = BookmarkedListingReviewFlow(
        selectedListing = reviewListing,
        isSubmitting = isSubmittingReview,
        errorMessage = reviewError,
        onOpen = {
            reviewError = null
            reviewListing = it
        },
        onDismiss = { if (!isSubmittingReview) reviewListing = null },
        onSubmit = submitReview
    )

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ProfileDestinationContent(
            destination,
            ProfileDestinationStates(profileState, editState, viewModel),
            flows,
            callbacks,
            themeViewModel,
            reviewFlow
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
    reviewFlow: BookmarkedListingReviewFlow
) {
    when (destination) {
        ProfileDestination.PROFILE -> ProfileLandingDestination(states.profile, callbacks) {
            states.viewModel.refreshProfile()
        }
        ProfileDestination.EDIT_ACCOUNT -> ProfileEditDestination(states.edit, flows, callbacks)
        ProfileDestination.SETTINGS -> ProfileSettingsDestination(states.profile, flows, callbacks, themeViewModel)
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
                    onListingClick = states.viewModel::viewBookmarkedListing,
                    onRemoveBookmark = states.viewModel::removeBookmarkedListing,
                    onRefresh = states.viewModel::loadBookmarkedListings,
                    onLoadMore = states.viewModel::loadMoreBookmarkedListings,
                    onCategorySelected = states.viewModel::onBookmarkedCategorySelected,
                    onConditionSelected = states.viewModel::onBookmarkedConditionSelected,
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
                        review = BookmarkedListingReviewActions(
                            isAvailable = listing.isSold && listing.buyerId == states.profile.profile?.id?.value,
                            onOpen = { reviewFlow.onOpen(listing) },
                            isOpen = reviewFlow.selectedListing?.id == listing.id,
                            isSubmitting = reviewFlow.isSubmitting,
                            errorMessage = reviewFlow.errorMessage,
                            onDismiss = reviewFlow.onDismiss,
                            onSubmit = reviewFlow.onSubmit
                        ),
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
            onBlockedUsers = { callbacks.onNavigate(ProfileDestination.BLOCKED_USERS) },
            onSignOut = { flows.account.signOut(callbacks.onSignOut) },
            onConfirmDeleteProfile = { flows.account.deleteProfile(callbacks.onSignOut) }
        ),
        themePreference = themeViewModel?.themePreference?.collectAsState()?.value,
        onThemePreferenceChanged = themeViewModel?.let { it::setThemePreference }
    )
}
