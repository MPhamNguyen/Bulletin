package com.jdrms.bulletin.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.app.di.AppContainer
import com.jdrms.bulletin.app.navigation.AppDestination
import com.jdrms.bulletin.app.navigation.AppRootScreen
import com.jdrms.bulletin.app.navigation.ProfileDestination
import com.jdrms.bulletin.app.navigation.backFromProfile
import com.jdrms.bulletin.app.theme.ThemePreference
import com.jdrms.bulletin.app.theme.ThemeViewModel
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.designsystem.BulletinTheme
import com.jdrms.bulletin.domain.home.presentation.HomeScreen
import com.jdrms.bulletin.domain.listings.presentation.ListingsScreen
import com.jdrms.bulletin.domain.listings.presentation.MyListingsScreen
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceScreen
import com.jdrms.bulletin.domain.messages.presentation.MessagesScreen
import com.jdrms.bulletin.domain.profile.presentation.AuthSessionState
import com.jdrms.bulletin.domain.profile.presentation.ProfileScreen
import com.jdrms.bulletin.domain.profile.presentation.ProfileViewModel
import com.jdrms.bulletin.domain.profile.presentation.SignInScreen
import com.jdrms.bulletin.domain.profile.presentation.SignUpScreen

@Composable
fun App(appContainer: AppContainer? = null) {
    val isInspectionMode = LocalInspectionMode.current
    val container = appContainer ?: remember { AppContainer(isInspectionMode = isInspectionMode) }
    val systemDarkTheme = isSystemInDarkTheme()
    val themeViewModel = remember { container.createThemeViewModel() }
    val themePreference by themeViewModel.themePreference.collectAsState()
    val isDarkTheme = resolveIsDarkTheme(themePreference, systemDarkTheme)

    BulletinTheme(darkTheme = isDarkTheme) {
        var currentRootScreen by remember { mutableStateOf(AppRootScreen.SIGN_IN) }
        val profileViewModel = remember { container.createProfileViewModel() }
        val profileUiState by profileViewModel.uiState.collectAsState()
        val effectiveRootScreen = resolveRootScreen(currentRootScreen, profileUiState.authSessionState)

        LaunchedEffect(profileUiState.authSessionState) {
            when (profileUiState.authSessionState) {
                AuthSessionState.CHECKING,
                AuthSessionState.AUTHENTICATED -> {
                    when (val result = container.getAuthenticatedUserId()) {
                        is Result.Success -> result.data?.let(themeViewModel::setAccount)
                        is Result.Error -> Unit
                    }
                }
                AuthSessionState.UNAUTHENTICATED -> themeViewModel.setAccount(null)
            }
        }

        LaunchedEffect(profileUiState.authSessionState) {
            currentRootScreen = resolveRootScreen(currentRootScreen, profileUiState.authSessionState)
        }

        if (profileUiState.authSessionState == AuthSessionState.CHECKING) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            when (effectiveRootScreen) {
                AppRootScreen.SIGN_IN -> {
                    SignInScreen(
                        errorMessage = profileUiState.errorMessage,
                        isLoading = profileUiState.isLoading,
                        onClearMessages = { profileViewModel.clearMessages() },
                        onSignIn = { email, password ->
                            profileViewModel.login(
                                emailStr = email,
                                pass = password,
                                onSuccess = {
                                    currentRootScreen = AppRootScreen.MAIN
                                }
                            )
                        },
                        onCreateAccount = {
                            profileViewModel.resetRegistration()
                            currentRootScreen = AppRootScreen.CREATE_PROFILE
                        }
                    )
                }
                AppRootScreen.CREATE_PROFILE -> {
                    SignUpScreen(
                        viewModel = profileViewModel,
                        onBack = {
                            profileViewModel.clearMessages()
                            currentRootScreen = AppRootScreen.SIGN_IN
                        },
                        onNavigateToSignIn = {
                            profileViewModel.clearMessages()
                            currentRootScreen = AppRootScreen.SIGN_IN
                        },
                        onContinueToApp = {
                            profileViewModel.clearMessages()
                            currentRootScreen = AppRootScreen.MAIN
                        }
                    )
                }
                AppRootScreen.MAIN -> {
                    MainAppScaffold(
                        appContainer = container,
                        profileViewModel = profileViewModel,
                        themeViewModel = themeViewModel,
                        darkTheme = isDarkTheme,
                        onSignOut = {
                            profileViewModel.signOut {
                                currentRootScreen = AppRootScreen.SIGN_IN
                            }
                        }
                    )
                }
            }
        }
    }
}

internal fun resolveRootScreen(
    currentRootScreen: AppRootScreen,
    authSessionState: AuthSessionState
): AppRootScreen {
    return when (authSessionState) {
        AuthSessionState.CHECKING -> currentRootScreen
        AuthSessionState.AUTHENTICATED -> AppRootScreen.MAIN
        AuthSessionState.UNAUTHENTICATED -> {
            if (currentRootScreen == AppRootScreen.MAIN) AppRootScreen.SIGN_IN else currentRootScreen
        }
    }
}

internal fun resolveIsDarkTheme(
    themePreference: ThemePreference,
    systemDarkTheme: Boolean
): Boolean = when (themePreference) {
    ThemePreference.SYSTEM -> systemDarkTheme
    ThemePreference.LIGHT -> false
    ThemePreference.DARK -> true
}

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
        var currentDestination by remember { mutableStateOf(AppDestination.HOME) }
        var profileDestination by remember { mutableStateOf(ProfileDestination.PROFILE) }

        val homeViewModel = remember { container.createHomeViewModel() }
        val marketplaceViewModel = remember { container.createMarketplaceViewModel() }
        val listingsViewModel = remember { container.createListingsViewModel() }
        val messagesViewModel = remember { container.createMessagesViewModel() }
        val resolvedProfileViewModel = remember(profileViewModel, container) {
            resolveProvidedOrCreate(profileViewModel) { container.createProfileViewModel() }
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            bottomBar = {
                BulletinBottomNavigationBar(
                    currentDestination = currentDestination,
                    onDestinationSelected = {
                        if (it == AppDestination.MESSAGES) messagesViewModel.loadConversations()
                        if (it != AppDestination.PROFILE) {
                            profileDestination = ProfileDestination.PROFILE
                            listingsViewModel.cancelEditing()
                            listingsViewModel.clearMessages()
                        }
                        currentDestination = it
                    }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                when (currentDestination) {
                    AppDestination.HOME -> HomeScreen(homeViewModel)
                    AppDestination.MARKETPLACE -> MarketplaceScreen(marketplaceViewModel)
                    AppDestination.LISTINGS -> ListingsScreen(listingsViewModel)
                    AppDestination.MESSAGES -> MessagesScreen(messagesViewModel)
                    AppDestination.PROFILE -> {
                        if (profileDestination != ProfileDestination.PROFILE) {
                            MyListingsScreen(
                                viewModel = listingsViewModel,
                                onBack = {
                                    listingsViewModel.clearMessages()
                                    listingsViewModel.cancelEditing()
                                    profileDestination = backFromProfile(profileDestination)
                                },
                                onEditListing = {
                                    profileDestination = ProfileDestination.EDIT_LISTING
                                }
                            )
                        } else {
                            ProfileScreen(
                                viewModel = resolvedProfileViewModel,
                                themeViewModel = themeViewModel,
                                onSignOut = onSignOut,
                                onMyListingsClick = {
                                    listingsViewModel.clearMessages()
                                    profileDestination = ProfileDestination.MY_LISTINGS
                                },
                                onCreateListingClick = {
                                    listingsViewModel.clearMessages()
                                    currentDestination = AppDestination.LISTINGS
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun <T> resolveProvidedOrCreate(provided: T?, create: () -> T): T = provided ?: create()

@Composable
fun BulletinBottomNavigationBar(
    currentDestination: AppDestination,
    onDestinationSelected: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column {
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(NavigationBarDefaults.windowInsets)
                    .height(64.dp)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppDestination.entries.forEach { destination ->
                    if (destination == AppDestination.LISTINGS) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                onClick = { onDestinationSelected(destination) },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                shadowElevation = 6.dp,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = destination.label,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        val isSelected = currentDestination == destination
                        val contentColor = if (isSelected) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = false, radius = 24.dp)
                                ) {
                                    onDestinationSelected(destination)
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                                tint = contentColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = destination.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = contentColor
                            )
                        }
                    }
                }
            }
        }
    }
}
