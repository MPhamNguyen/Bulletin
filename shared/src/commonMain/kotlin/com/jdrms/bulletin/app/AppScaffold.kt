package com.jdrms.bulletin.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.app.di.AppContainer
import com.jdrms.bulletin.app.di.createHomeViewModel
import com.jdrms.bulletin.app.di.createListingReportViewModel
import com.jdrms.bulletin.app.di.createListingsViewModel
import com.jdrms.bulletin.app.di.createMarketplaceUserBlockViewModel
import com.jdrms.bulletin.app.di.createMarketplaceUserReportViewModel
import com.jdrms.bulletin.app.di.createMarketplaceViewModel
import com.jdrms.bulletin.app.di.createMessagesViewModel
import com.jdrms.bulletin.app.di.createProfileViewModel
import com.jdrms.bulletin.app.di.createSellerProfileViewModel
import com.jdrms.bulletin.app.navigation.AppDestination
import com.jdrms.bulletin.app.navigation.MutableMainNavigationState
import com.jdrms.bulletin.app.navigation.ProfileDestination
import com.jdrms.bulletin.app.theme.ThemeViewModel
import com.jdrms.bulletin.core.designsystem.BulletinTheme
import com.jdrms.bulletin.domain.home.presentation.HomeSaveRetryNavigation
import com.jdrms.bulletin.domain.home.presentation.HomeSaveRetryPresentation
import com.jdrms.bulletin.domain.home.presentation.HomeViewModel
import com.jdrms.bulletin.domain.listings.presentation.ListingsViewModel
import com.jdrms.bulletin.domain.marketplace.presentation.ListingReportViewModel
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceUserBlockViewModel
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceUserReportViewModel
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceViewModel
import com.jdrms.bulletin.domain.marketplace.presentation.SellerProfileViewModel
import com.jdrms.bulletin.domain.messages.presentation.MessagesViewModel
import com.jdrms.bulletin.domain.profile.presentation.ProfileViewModel
import kotlinx.coroutines.cancel

internal class ProfilePresentation(
    val viewModel: ProfileViewModel,
    val themeViewModel: ThemeViewModel?,
    val onSignOut: () -> Unit
)

internal class MainScaffoldModels(
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
    val chrome = rememberMainAppChrome()
    val models = rememberMainScaffoldModels(container)
    val profile = rememberProfilePresentation(profileViewModel, container, themeViewModel, onSignOut)
    val retries = rememberSaveable(saver = HomeSaveRetryPresentation.saver) { HomeSaveRetryPresentation() }
    LaunchedEffect(navigation.currentDestination) { chrome.behavior.onDestinationChanged() }
    MainHomeEffects(navigation, models, retries)
    BulletinTheme(darkTheme = darkTheme) {
        MainAppLayout(MainAppContentState(container, navigation, models, profile), chrome, retries)
    }
}

@Composable
private fun rememberMainScaffoldModels(container: AppContainer): MainScaffoldModels {
    return remember(container) {
        MainScaffoldModels(
            container.home.createHomeViewModel(container.profile.sessionRepository, container.listingChangedSignal),
            container.marketplace.createMarketplaceViewModel(container.listingChangedSignal),
            container.marketplace.createSellerProfileViewModel(),
            container.marketplace.createMarketplaceUserReportViewModel(),
            container.marketplace.createListingReportViewModel(),
            container.marketplace.createMarketplaceUserBlockViewModel(container.listingChangedSignal),
            container.listings.createListingsViewModel(
                container.messages.recentListingBuyersProvider,
                container.listingChangedSignal
            ),
            container.messages.createMessagesViewModel(
                container.profile.conversationUserBlocker(container.listingChangedSignal)
            )
        )
    }
}

@Composable
private fun rememberProfilePresentation(
    profileViewModel: ProfileViewModel?,
    container: AppContainer,
    themeViewModel: ThemeViewModel?,
    onSignOut: () -> Unit
): ProfilePresentation {
    // Keep the profile read model alive for the authenticated app session.
    val resolvedProfileViewModel = remember(profileViewModel, container) {
        resolveProvidedOrCreate(profileViewModel) {
            container.profile.createProfileViewModel(
                container.listings,
                container.marketplace,
                container.listingChangedSignal
            )
        }
    }
    DisposableEffect(resolvedProfileViewModel, profileViewModel) {
        onDispose {
            if (profileViewModel == null) resolvedProfileViewModel.viewModelScope.cancel()
        }
    }
    return ProfilePresentation(resolvedProfileViewModel, themeViewModel, onSignOut)
}

@Composable
private fun MainHomeEffects(
    navigation: MutableMainNavigationState,
    models: MainScaffoldModels,
    saveRetries: HomeSaveRetryPresentation
) {
    val homeState by models.home.uiState.collectAsState()
    HomeSaveRetryNavigation(navigation.currentDestination == AppDestination.HOME, homeState, saveRetries, models.home)
    LaunchedEffect(navigation.currentDestination, homeState.interaction.pendingEffects) {
        if (navigation.currentDestination != AppDestination.HOME) {
            homeState.interaction.pendingEffects.filterIsInstance<
                com.jdrms.bulletin.domain.home.presentation.HomeEffect.SaveConfirmed
                >().forEach { models.home.onEffectConsumed(it.id) }
        }
    }
}

@Composable
private fun rememberMainAppChrome(): MainAppChrome {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    return remember(density, scope, haptics) {
        val behavior = NavbarScrollBehavior(with(density) { NAVBAR_SCROLL_THRESHOLD_DP.dp.toPx() })
        MainAppChrome(
            behavior,
            NavbarNestedScrollConnection(behavior),
            ProfileTabAnchor(),
            ProfileTabCelebration(),
            scope,
            haptics
        )
    }
}

private const val NAVBAR_SCROLL_THRESHOLD_DP = 32

internal fun navigateTo(
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

internal fun <T> resolveProvidedOrCreate(provided: T?, create: () -> T): T = provided ?: create()
