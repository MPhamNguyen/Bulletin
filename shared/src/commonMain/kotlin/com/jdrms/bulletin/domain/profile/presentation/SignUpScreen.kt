package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SignUpScreen(
    viewModel: RegistrationViewModel,
    onBack: () -> Unit = {},
    onNavigateToSignIn: () -> Unit = onBack,
    onContinueToApp: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val horizontalPadding = when {
            maxWidth >= 600.dp -> 40.dp
            maxWidth >= 480.dp -> 32.dp
            else -> 20.dp
        }
        val verticalPadding = if (maxHeight < 700.dp) 12.dp else 24.dp

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SignUpTopBar(
                title = if (uiState.stage is RegistrationStage.Complete) "Account Created" else "Sign up",
                showBackButton = uiState.stage !is RegistrationStage.Complete,
                horizontalPadding = horizontalPadding,
                onBackClick = {
                    viewModel.clearError()
                    onBack()
                }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SignUpLogoHeader(verticalPadding)

                when (val stage = uiState.stage) {
                    is RegistrationStage.Complete -> SignUpSuccessContent(
                        profile = stage.profile,
                        successMessage = "Account created successfully!",
                        onContinueToApp = onContinueToApp,
                        onNavigateToSignIn = onNavigateToSignIn
                    )

                    is RegistrationStage.Recovery -> EmailVerificationRecoveryContent(
                        uiState = uiState,
                        email = stage.email,
                        onRetry = viewModel::retryProfile,
                        onSignIn = {
                            viewModel.reset()
                            onNavigateToSignIn()
                        }
                    )

                    is RegistrationStage.Verification -> EmailVerificationContent(
                        uiState = uiState,
                        pending = stage.pending,
                        onVerify = viewModel::verify,
                        onResend = viewModel::resendCode,
                        onChangeEmail = viewModel::reset
                    )

                    RegistrationStage.Form -> SignUpFormContent(
                        uiState = uiState,
                        onClearMessages = viewModel::clearError,
                        onCreateAccount = { firstName, lastName, email, password ->
                            viewModel.register(firstName, lastName, email, password)
                        },
                        onNavigateToSignIn = {
                            viewModel.clearError()
                            onNavigateToSignIn()
                        }
                    )
                }
            }
        }
    }
}
