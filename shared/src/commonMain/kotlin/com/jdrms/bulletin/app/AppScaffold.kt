package com.jdrms.bulletin.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.app.di.AppContainer
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
import com.jdrms.bulletin.app.theme.ThemeViewModel
import com.jdrms.bulletin.core.designsystem.BulletinExtras
import com.jdrms.bulletin.core.designsystem.BulletinTheme
import com.jdrms.bulletin.core.designsystem.LocalNavbarHeight
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

@OptIn(ExperimentalLayoutApi::class)
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
    val profileTabAnchor = remember { ProfileTabAnchor() }
    val profileCelebration = remember { ProfileTabCelebration() }
    val celebrationScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val keyboardVisible = WindowInsets.isImeVisible
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

    val homeState by models.home.uiState.collectAsState()
    LaunchedEffect(navigation.currentDestination, homeState.interaction.pendingEffects) {
        if (navigation.currentDestination != AppDestination.HOME) {
            homeState.interaction.pendingEffects.filterIsInstance<
                com.jdrms.bulletin.domain.home.presentation.HomeEffect.SaveConfirmed
                >().forEach { models.home.onEffectConsumed(it.id) }
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
                            onContentNoLongerScrollable = navbarScrollBehavior::reveal,
                            profileAnchorInWindow = profileTabAnchor.bounds(
                                navigation.showsBottomNavigation && navbarScrollBehavior.isVisible && !keyboardVisible
                            ),
                            onSaveCelebration = { scale -> profileCelebration.play(celebrationScope, scale, haptics) }
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
                profileIconModifier = profileTabAnchor.modifier.then(
                    profileCelebration.modifier(BulletinExtras.colors.success)
                ),
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

internal fun <T> resolveProvidedOrCreate(provided: T?, create: () -> T): T = provided ?: create()
