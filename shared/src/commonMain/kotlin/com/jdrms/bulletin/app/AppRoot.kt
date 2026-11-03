package com.jdrms.bulletin.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.app.di.AppContainer
import com.jdrms.bulletin.app.di.createRegistrationViewModel
import com.jdrms.bulletin.app.navigation.AppRootScreen
import com.jdrms.bulletin.app.theme.ThemeViewModel
import com.jdrms.bulletin.domain.profile.presentation.AuthSessionState
import com.jdrms.bulletin.domain.profile.presentation.PasswordRecoveryViewModel
import com.jdrms.bulletin.domain.profile.presentation.SignUpScreen
import kotlinx.coroutines.cancel

@Composable
internal fun AppRoot(
    container: AppContainer,
    theme: ThemeViewModel,
    recovery: PasswordRecoveryViewModel
) {
    var currentScreen by remember { mutableStateOf(AppRootScreen.SIGN_IN) }
    val session by container.profile.sessionRepository.state.collectAsState()
    val authState = session.toAuthSessionState()
    LaunchedEffect(container) { container.profile.sessionRepository.restore() }
    LaunchedEffect(authState) { currentScreen = resolveRootScreen(currentScreen, authState) }
    if (authState == AuthSessionState.CHECKING) {
        Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else {
        when (resolveRootScreen(currentScreen, authState)) {
            AppRootScreen.SIGN_IN -> SignInRoute(container, recovery) { currentScreen = AppRootScreen.CREATE_PROFILE }
            AppRootScreen.CREATE_PROFILE -> RegistrationRoute(container) { currentScreen = it }
            AppRootScreen.MAIN -> MainAppScaffold(
                appContainer = container,
                themeViewModel = theme
            ) { currentScreen = AppRootScreen.SIGN_IN }
        }
    }
}

@Composable
private fun RegistrationRoute(container: AppContainer, navigate: (AppRootScreen) -> Unit) {
    val viewModel = remember(container) { container.profile.createRegistrationViewModel() }
    DisposableEffect(viewModel) { onDispose { viewModel.viewModelScope.cancel() } }
    val signIn = {
        viewModel.clearError()
        navigate(AppRootScreen.SIGN_IN)
    }
    SignUpScreen(
        viewModel = viewModel,
        onBack = signIn,
        onNavigateToSignIn = signIn,
        onContinueToApp = {
            viewModel.clearError()
            navigate(AppRootScreen.MAIN)
        }
    )
}
