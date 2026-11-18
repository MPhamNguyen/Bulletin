package com.jdrms.bulletin.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.app.di.AppContainer
import com.jdrms.bulletin.app.di.createAccountViewModel
import com.jdrms.bulletin.app.di.createBlockedUsersViewModel
import com.jdrms.bulletin.app.di.createEditProfileViewModel
import com.jdrms.bulletin.app.navigation.MutableMainNavigationState
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceReporterId
import com.jdrms.bulletin.domain.profile.presentation.ProfileScreen
import com.jdrms.bulletin.domain.profile.presentation.ProfileScreenFlows
import com.jdrms.bulletin.domain.profile.presentation.ProfileScreenOptions
import com.jdrms.bulletin.domain.profile.presentation.ReportedListingVisibilitySetting
import kotlinx.coroutines.cancel

@Composable
internal fun StudentProfileDestination(
    container: AppContainer,
    navigation: MutableMainNavigationState,
    models: MainScaffoldModels,
    profilePresentation: ProfilePresentation,
    interestNotificationCount: Int
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
            models.listings,
            models.marketplace,
            models.messages,
            profilePresentation.onSignOut
        ).copy(
            onViewAllReviews = { profileState.profile?.id?.value?.let(navigation::openSellerReviews) }
        ),
        options = ProfileScreenOptions(
            interestNotificationCount = interestNotificationCount,
            themeViewModel = profilePresentation.themeViewModel,
            reportedListingVisibilitySetting = ReportedListingVisibilitySetting(
                hideReportedListings = hideReportedListings,
                onChanged = { shouldHide ->
                    profilePresentation.reportedListingVisibilityViewModel.setHideReportedListings(shouldHide) {
                        container.listingChangedSignal.emit()
                    }
                }
            ),
            profileReviewsContent = { onViewAllReviews ->
                ProfileReviewsIntegration(
                    profileState = profileState,
                    onViewAllReviews = onViewAllReviews,
                    onListingClick = { listingId ->
                        models.marketplace.onListingClicked(listingId)
                        navigation.openListing(listingId)
                    }
                )
            }
        ),
    )
}
