package com.jdrms.bulletin.app

import androidx.compose.runtime.*
import com.jdrms.bulletin.app.di.AppContainer
import com.jdrms.bulletin.domain.profile.presentation.*

@Composable
internal fun SignInRoute(container: AppContainer, recovery: PasswordRecoveryViewModel, onCreateAccount: () -> Unit) {
    val viewModel = container.signInViewModel
    LaunchedEffect(viewModel) {
        if (container.externalSignInPending) viewModel.signInWithMicrosoft()
    }
    val state by viewModel.uiState.collectAsState()
    val recoveryState by recovery.uiState.collectAsState()
    if (recoveryState.stage == PasswordRecoveryStage.NONE) {
        SignInScreen(
            errorMessage = state.errorMessage,
            isLoading = state.isLoading,
            microsoftSignInEnabled = container.microsoftSignInEnabled,
            externalStage = state.externalStage,
            actions = SignInScreenActions(
                onClearMessages = viewModel::clearError,
                onSignIn = viewModel::signIn,
                onMicrosoftSignIn = viewModel::signInWithMicrosoft,
                onForgotPassword = recovery::beginPasswordReset,
                onCreateAccount = onCreateAccount
            )
        )
    } else {
        PasswordRecoveryRoute(recoveryState, recovery)
    }
}

@Composable
private fun PasswordRecoveryRoute(state: PasswordRecoveryUiState, viewModel: PasswordRecoveryViewModel) {
    when (state.stage) {
        PasswordRecoveryStage.NONE -> Unit
        PasswordRecoveryStage.ENTER_EMAIL -> ForgotPasswordScreen(
            errorMessage = state.errorMessage,
            isLoading = state.isLoading,
            onSubmit = viewModel::requestPasswordReset,
            onBack = viewModel::cancelPasswordReset
        )
        PasswordRecoveryStage.ENTER_CODE -> PasswordConfirmationCodeScreen(
            email = state.email,
            errorMessage = state.errorMessage,
            isLoading = state.isLoading,
            onSubmit = viewModel::verifyPasswordResetCode,
            onBack = viewModel::beginPasswordReset
        )
        PasswordRecoveryStage.CHANGE_PASSWORD -> ChangePasswordScreen(
            errorMessage = state.errorMessage,
            isLoading = state.isLoading,
            onSubmit = viewModel::updatePassword,
            onBack = viewModel::cancelPasswordReset
        )
    }
}
