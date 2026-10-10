package com.jdrms.bulletin.app

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.app.di.AppContainer
import com.jdrms.bulletin.app.navigation.AppDestination
import com.jdrms.bulletin.app.navigation.AppRootScreen
import com.jdrms.bulletin.app.navigation.MainNavigationState
import com.jdrms.bulletin.app.navigation.ProfileDestination
import com.jdrms.bulletin.app.navigation.backFromProfile
import com.jdrms.bulletin.app.navigation.backFromProfileDestination
import com.jdrms.bulletin.app.theme.ThemePreference
import com.jdrms.bulletin.app.theme.ThemeViewModel
import com.jdrms.bulletin.core.designsystem.BulletinTheme
import com.jdrms.bulletin.domain.home.presentation.HomeScreen
import com.jdrms.bulletin.domain.listings.presentation.ListingsScreen
import com.jdrms.bulletin.domain.listings.presentation.MyListingsScreen
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceScreen
import com.jdrms.bulletin.domain.messages.presentation.MessagesScreen
import com.jdrms.bulletin.domain.profile.application.SessionState
import com.jdrms.bulletin.domain.profile.presentation.AuthSessionState
import com.jdrms.bulletin.domain.profile.presentation.ChangePasswordScreen
import com.jdrms.bulletin.domain.profile.presentation.ForgotPasswordScreen
import com.jdrms.bulletin.domain.profile.presentation.PasswordConfirmationCodeScreen
import com.jdrms.bulletin.domain.profile.presentation.PasswordRecoveryStage
import com.jdrms.bulletin.domain.profile.presentation.ProfileScreen
import com.jdrms.bulletin.domain.profile.presentation.ProfileScreenCallbacks
import com.jdrms.bulletin.domain.profile.presentation.ProfileScreenFlows
import com.jdrms.bulletin.domain.profile.presentation.ProfileViewModel
import com.jdrms.bulletin.domain.profile.presentation.SignInScreen
import com.jdrms.bulletin.domain.profile.presentation.SignUpScreen
import kotlinx.coroutines.cancel

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
        val sessionState by container.sessionRepository.state.collectAsState()
        val authSessionState = sessionState.toAuthSessionState()
        val passwordRecoveryViewModel = remember { container.createPasswordRecoveryViewModel() }
        val passwordRecoveryUiState by passwordRecoveryViewModel.uiState.collectAsState()
        val effectiveRootScreen = resolveRootScreen(currentRootScreen, authSessionState)

        LaunchedEffect(container) { container.sessionRepository.restore() }

        LaunchedEffect(sessionState) {
            when (val session = sessionState) {
                is SessionState.Authenticated -> themeViewModel.setAccount(session.profile.id)
                SessionState.Unauthenticated -> themeViewModel.setAccount(null)
                SessionState.Checking -> Unit
            }
        }

        LaunchedEffect(authSessionState) {
            currentRootScreen = resolveRootScreen(currentRootScreen, authSessionState)
        }

        if (authSessionState == AuthSessionState.CHECKING) {
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
                    val signInViewModel = remember(container) { container.createSignInViewModel() }
                    DisposableEffect(signInViewModel) {
                        onDispose { signInViewModel.viewModelScope.cancel() }
                    }
                    val signInUiState by signInViewModel.uiState.collectAsState()
                    when (passwordRecoveryUiState.stage) {
                        PasswordRecoveryStage.NONE -> SignInScreen(
                            errorMessage = signInUiState.errorMessage,
                            isLoading = signInUiState.isLoading,
                            onClearMessages = signInViewModel::clearError,
                            onSignIn = signInViewModel::signIn,
                            onForgotPassword = { passwordRecoveryViewModel.beginPasswordReset() },
                            onCreateAccount = {
                                currentRootScreen = AppRootScreen.CREATE_PROFILE
                            }
                        )
                        PasswordRecoveryStage.ENTER_EMAIL -> ForgotPasswordScreen(
                            errorMessage = passwordRecoveryUiState.errorMessage,
                            isLoading = passwordRecoveryUiState.isLoading,
                            onSubmit = passwordRecoveryViewModel::requestPasswordReset,
                            onBack = passwordRecoveryViewModel::cancelPasswordReset
                        )
                        PasswordRecoveryStage.ENTER_CODE -> PasswordConfirmationCodeScreen(
                            email = passwordRecoveryUiState.email,
                            errorMessage = passwordRecoveryUiState.errorMessage,
                            isLoading = passwordRecoveryUiState.isLoading,
                            onSubmit = passwordRecoveryViewModel::verifyPasswordResetCode,
                            onBack = passwordRecoveryViewModel::beginPasswordReset
                        )
                        PasswordRecoveryStage.CHANGE_PASSWORD -> ChangePasswordScreen(
                            errorMessage = passwordRecoveryUiState.errorMessage,
                            isLoading = passwordRecoveryUiState.isLoading,
                            onSubmit = passwordRecoveryViewModel::updatePassword,
                            onBack = passwordRecoveryViewModel::cancelPasswordReset
                        )
                    }
                }
                AppRootScreen.CREATE_PROFILE -> {
                    val registrationViewModel = remember(container) { container.createRegistrationViewModel() }
                    DisposableEffect(registrationViewModel) {
                        onDispose { registrationViewModel.viewModelScope.cancel() }
                    }
                    SignUpScreen(
                        viewModel = registrationViewModel,
                        onBack = {
                            registrationViewModel.clearError()
                            currentRootScreen = AppRootScreen.SIGN_IN
                        },
                        onNavigateToSignIn = {
                            registrationViewModel.clearError()
                            currentRootScreen = AppRootScreen.SIGN_IN
                        },
                        onContinueToApp = {
                            registrationViewModel.clearError()
                            currentRootScreen = AppRootScreen.MAIN
                        }
                    )
                }
                AppRootScreen.MAIN -> {
                    MainAppScaffold(
                        appContainer = container,
                        themeViewModel = themeViewModel,
                        darkTheme = isDarkTheme,
                        onSignOut = { currentRootScreen = AppRootScreen.SIGN_IN }
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
        AuthSessionState.AUTHENTICATED -> {
            if (currentRootScreen == AppRootScreen.CREATE_PROFILE) AppRootScreen.CREATE_PROFILE else AppRootScreen.MAIN
        }
        AuthSessionState.UNAUTHENTICATED -> {
            if (currentRootScreen == AppRootScreen.MAIN) AppRootScreen.SIGN_IN else currentRootScreen
        }
    }
}

private fun SessionState.toAuthSessionState(): AuthSessionState = when (this) {
    SessionState.Checking -> AuthSessionState.CHECKING
    SessionState.Unauthenticated -> AuthSessionState.UNAUTHENTICATED
    is SessionState.Authenticated -> AuthSessionState.AUTHENTICATED
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
        var navigationState by remember { mutableStateOf(MainNavigationState()) }
        var editReturnDestination by remember { mutableStateOf(ProfileDestination.PROFILE) }

        val homeViewModel = remember { container.createHomeViewModel() }
        val marketplaceViewModel = remember { container.createMarketplaceViewModel() }
        val listingsViewModel = remember { container.createListingsViewModel() }
        val messagesViewModel = remember { container.createMessagesViewModel() }

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
                        navigationState = navigationState.selectBottomNavigationDestination(destination)
                    }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                when (navigationState.currentDestination) {
                    AppDestination.HOME -> HomeScreen(homeViewModel)
                    AppDestination.MARKETPLACE -> MarketplaceScreen(marketplaceViewModel)
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
