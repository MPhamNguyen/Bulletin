package com.jdrms.bulletin.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.jdrms.bulletin.app.di.AppContainer
import com.jdrms.bulletin.app.di.createPasswordRecoveryViewModel
import com.jdrms.bulletin.app.navigation.AppRootScreen
import com.jdrms.bulletin.app.theme.ThemePreference
import com.jdrms.bulletin.core.designsystem.theme.BulletinTheme
import com.jdrms.bulletin.domain.profile.application.SessionState
import com.jdrms.bulletin.domain.profile.presentation.AuthSessionState
import com.jdrms.bulletin.domain.profile.presentation.PasswordRecoveryStage

@Composable
fun App(appContainer: AppContainer? = null) {
    val isInspectionMode = LocalInspectionMode.current
    val container = appContainer ?: remember { AppContainer(isInspectionMode = isInspectionMode) }
    DisposableEffect(container) {
        // Android supplies an application-owned container that survives Activity recreation.
        onDispose { if (appContainer == null) container.messages.inboxStateOwner.close() }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { container.messages.inboxStateOwner.onForeground() }
    val themeViewModel = remember(container) { container.createThemeViewModel() }
    val preference by themeViewModel.themePreference.collectAsState()
    val recovery = remember(container) { container.profile.createPasswordRecoveryViewModel() }
    val recoveryState by recovery.uiState.collectAsState()
    val isDarkTheme = resolveAppIsDarkTheme(
        preference,
        isSystemInDarkTheme(),
        recoveryState.stage != PasswordRecoveryStage.NONE
    )
    val session by container.profile.sessionRepository.state.collectAsState()
    LaunchedEffect(session) {
        when (val current = session) {
            is SessionState.Authenticated -> themeViewModel.setAccount(current.profile.id)
            SessionState.Unauthenticated -> themeViewModel.setAccount(null)
            SessionState.Checking -> Unit
        }
    }
    BulletinTheme(darkTheme = isDarkTheme) {
        AppRoot(container, themeViewModel, isDarkTheme, recovery)
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

internal fun SessionState.toAuthSessionState(): AuthSessionState = when (this) {
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

internal fun resolveAppIsDarkTheme(
    themePreference: ThemePreference,
    systemDarkTheme: Boolean,
    isPasswordRecovery: Boolean
): Boolean = if (isPasswordRecovery) {
    systemDarkTheme
} else {
    resolveIsDarkTheme(themePreference, systemDarkTheme)
}
