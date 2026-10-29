package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonDefaults
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

data class SignInScreenActions(
    val onEmailChange: ((String) -> Unit)? = null,
    val onPasswordChange: ((String) -> Unit)? = null,
    val onClearMessages: () -> Unit = {},
    val onSignIn: (String, String) -> Unit = { _, _ -> },
    val onMicrosoftSignIn: () -> Unit = {},
    val onForgotPassword: () -> Unit = {},
    val onCreateAccount: () -> Unit = {}
)

@Composable
fun SignInScreen(
    email: String? = null,
    password: String? = null,
    errorMessage: String? = null,
    isLoading: Boolean = false,
    microsoftSignInEnabled: Boolean = false,
    externalStage: ExternalSignInStage = ExternalSignInStage.Idle,
    actions: SignInScreenActions = SignInScreenActions()
) {
    var localEmail by remember { mutableStateOf("") }
    var localPassword by remember { mutableStateOf("") }
    val fieldActions = actions.copy(
        onEmailChange = {
            localEmail = it
            actions.onEmailChange?.invoke(it)
        },
        onPasswordChange = {
            localPassword = it
            actions.onPasswordChange?.invoke(it)
        }
    )
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = BulletinSpacing.xl),
        contentAlignment = Alignment.Center
    ) {
        SignInContent(
            email ?: localEmail,
            password ?: localPassword,
            errorMessage,
            isLoading,
            microsoftSignInEnabled,
            externalStage,
            fieldActions
        )
    }
}

@Composable
private fun SignInContent(
    email: String,
    password: String,
    errorMessage: String?,
    isLoading: Boolean,
    microsoftSignInEnabled: Boolean,
    externalStage: ExternalSignInStage,
    actions: SignInScreenActions
) {
    Column(
        Modifier.fillMaxWidth().widthIn(max = 480.dp).verticalScroll(rememberScrollState()).padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        SignInHeader()
        SignInError(errorMessage)
        SignInEmailField(email, { actions.onEmailChange?.invoke(it) }, actions.onClearMessages)
        SignInPasswordField(password, { actions.onPasswordChange?.invoke(it) }, actions.onClearMessages)
        PasswordSignInActions(isLoading, actions.onForgotPassword) { actions.onSignIn(email, password) }
        MicrosoftSignInAction(microsoftSignInEnabled, !isLoading, actions.onMicrosoftSignIn, externalStage)
        Spacer(Modifier.height(28.dp))
        Text(
            "New to Bulletin?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = actions.onCreateAccount, colors = BulletinButtonDefaults.textButtonColors()) {
            Text("Create an account")
        }
    }
}

@Composable
private fun SignInHeader() {
    BulletinMark()
    Spacer(Modifier.height(BulletinSpacing.xl))
    Text(
        "Welcome to Bulletin",
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(BulletinSpacing.s))
    Text(
        "Sign in to your campus marketplace",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(BulletinSpacing.xl))
}

@Composable
private fun SignInError(message: String?) {
    message?.let {
        Box(
            Modifier.fillMaxWidth().background(
                MaterialTheme.colorScheme.errorContainer,
                MaterialTheme.shapes.medium
            ).padding(BulletinSpacing.m)
        ) {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
        Spacer(Modifier.height(BulletinSpacing.l))
    }
}

@Composable
private fun PasswordSignInActions(isLoading: Boolean, onForgotPassword: () -> Unit, onSignIn: () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        TextButton(
            onClick = onForgotPassword,
            enabled = !isLoading,
            colors = BulletinButtonDefaults.textButtonColors()
        ) { Text("Forgot password?") }
    }
    Spacer(Modifier.height(BulletinSpacing.m))
    Button(
        onClick = onSignIn,
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = BulletinButtonDefaults.buttonColors()
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text("Sign in", style = MaterialTheme.typography.titleMedium)
        }
    }
}
