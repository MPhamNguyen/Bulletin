package com.jdrms.bulletin.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.jdrms.bulletin.core.designsystem.BulletinTheme
import com.jdrms.bulletin.domain.home.presentation.HomeScreen
import com.jdrms.bulletin.domain.listings.presentation.ListingsScreen
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

    BulletinTheme {
        var currentRootScreen by remember { mutableStateOf(AppRootScreen.SIGN_IN) }
        val profileViewModel = remember { container.createProfileViewModel() }
        val profileUiState by profileViewModel.uiState.collectAsState()
        val effectiveRootScreen = resolveRootScreen(currentRootScreen, profileUiState.authSessionState)

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

@Composable
fun MainAppScaffold(
    appContainer: AppContainer? = null,
    profileViewModel: ProfileViewModel? = null,
    onSignOut: () -> Unit = {}
) {
    val isInspectionMode = LocalInspectionMode.current
    val container = appContainer ?: remember { AppContainer(isInspectionMode = isInspectionMode) }

    BulletinTheme {
        var currentDestination by remember { mutableStateOf(AppDestination.HOME) }

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
                    onDestinationSelected = { currentDestination = it }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                when (currentDestination) {
                    AppDestination.HOME -> HomeScreen(homeViewModel)
                    AppDestination.MARKETPLACE -> MarketplaceScreen(marketplaceViewModel)
                    AppDestination.LISTINGS -> ListingsScreen(listingsViewModel)
                    AppDestination.MESSAGES -> MessagesScreen(messagesViewModel)
                    AppDestination.PROFILE -> ProfileScreen(
                        viewModel = resolvedProfileViewModel,
                        onSignOut = onSignOut
                    )
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
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 8.dp
    ) {
        Column {
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(NavigationBarDefaults.windowInsets)
                    .height(68.dp)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
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
                                shadowElevation = if (currentDestination == destination) 4.dp else 2.dp,
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = destination.label,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        val isSelected = currentDestination == destination
                        val contentColor = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
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
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = destination.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = contentColor
                            )
                        }
                    }
                }
            }
        }
    }
}
