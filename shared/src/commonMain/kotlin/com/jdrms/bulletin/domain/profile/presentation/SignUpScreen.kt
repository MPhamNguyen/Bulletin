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
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile

@Composable
fun SignUpScreen(
    viewModel: ProfileViewModel,
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
                title = if (uiState.isAccountCreated) "Account Created" else "Sign up",
                showBackButton = !uiState.isAccountCreated,
                horizontalPadding = horizontalPadding,
                onBackClick = {
                    viewModel.clearMessages()
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

                val createdProfile: StudentProfile? = uiState.profile
                val pendingRegistration = uiState.pendingRegistration
                val verifiedEmailAwaitingProfile = uiState.verifiedEmailAwaitingProfile
                when {
                    uiState.isAccountCreated && createdProfile != null -> SignUpSuccessContent(
                        profile = createdProfile,
                        successMessage = uiState.successMessage,
                        onContinueToApp = onContinueToApp,
                        onNavigateToSignIn = onNavigateToSignIn
                    )

                    verifiedEmailAwaitingProfile != null -> EmailVerificationRecoveryContent(
                        uiState = uiState,
                        onRetry = viewModel::retryVerifiedProfile,
                        onSignIn = {
                            viewModel.resetRegistration()
                            onNavigateToSignIn()
                        }
                    )

                    pendingRegistration != null -> EmailVerificationContent(
                        uiState = uiState,
                        onVerify = { code -> viewModel.verifyEmail(pendingRegistration.email.value, code) },
                        onResend = { viewModel.resendEmailCode(pendingRegistration.email.value) },
                        onChangeEmail = viewModel::resetRegistration
                    )

                    else -> SignUpFormContent(
                        uiState = uiState,
                        onClearMessages = viewModel::clearMessages,
                        onCreateAccount = viewModel::createAccount,
                        onNavigateToSignIn = {
                            viewModel.clearMessages()
                            onNavigateToSignIn()
                        }
                    )
                }
            }
        }
    }
}
