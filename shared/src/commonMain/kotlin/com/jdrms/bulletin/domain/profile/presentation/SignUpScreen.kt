package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import bulletin.shared.generated.resources.Res
import bulletin.shared.generated.resources.ic_arrow_back
import bulletin.shared.generated.resources.ic_graduation_cap
import bulletin.shared.generated.resources.ic_visibility
import bulletin.shared.generated.resources.ic_visibility_off
import com.jdrms.bulletin.core.designsystem.BulletinButtonDefaults
import com.jdrms.bulletin.core.designsystem.BulletinExtras
import com.jdrms.bulletin.core.designsystem.BulletinTextFieldDefaults
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import org.jetbrains.compose.resources.painterResource

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

                val createdProfile = uiState.profile
                val pendingRegistration = uiState.pendingRegistration
                val verifiedEmailAwaitingProfile = uiState.verifiedEmailAwaitingProfile
                if (uiState.isAccountCreated && createdProfile != null) {
                    SignUpSuccessContent(
                        profile = createdProfile,
                        successMessage = uiState.successMessage,
                        onContinueToApp = onContinueToApp,
                        onNavigateToSignIn = onNavigateToSignIn
                    )
                } else if (verifiedEmailAwaitingProfile != null) {
                    EmailVerificationRecoveryContent(
                        uiState = uiState,
                        onRetry = viewModel::retryVerifiedProfile,
                        onSignIn = {
                            viewModel.resetRegistration()
                            onNavigateToSignIn()
                        }
                    )
                } else if (pendingRegistration != null) {
                    EmailVerificationContent(
                        uiState = uiState,
                        onVerify = { viewModel.verifyEmail(pendingRegistration.email.value, it) },
                        onResend = { viewModel.resendEmailCode(pendingRegistration.email.value) },
                        onChangeEmail = { viewModel.resetRegistration() }
                    )
                } else {
                    SignUpFormContent(
                        uiState = uiState,
                        onClearMessages = { viewModel.clearMessages() },
                        onCreateAccount = { first, last, mail, pass ->
                            viewModel.createAccount(first, last, mail, pass)
                        },
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

@Composable
private fun SignUpTopBar(
    title: String,
    showBackButton: Boolean = true,
    horizontalPadding: Dp,
    onBackClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center
    ) {
        if (showBackButton) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SignUpLogoHeader(verticalPadding: Dp) {
    Spacer(modifier = Modifier.height(verticalPadding))

    Box(
        modifier = Modifier
            .size(96.dp)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_graduation_cap),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(54.dp)
        )
    }

    Spacer(modifier = Modifier.height(verticalPadding * 0.75f))
}

@Composable
private fun SignUpSuccessContent(
    profile: StudentProfile,
    successMessage: String?,
    onContinueToApp: () -> Unit,
    onNavigateToSignIn: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = BulletinExtras.colors.successContainer,
                shape = MaterialTheme.shapes.medium
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = successMessage ?: "Account created successfully!",
            style = MaterialTheme.typography.titleMedium,
            color = BulletinExtras.colors.onSuccessContainer,
            fontWeight = FontWeight.SemiBold
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = "Welcome, ${profile.fullName}!",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = profile.email.value,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = profile.university,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.secondary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(28.dp))

    Button(
        onClick = onContinueToApp,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = BulletinButtonDefaults.buttonColors()
    ) {
        Text(
            text = "Continue to Bulletin",
            style = MaterialTheme.typography.titleMedium
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedButton(
        onClick = onNavigateToSignIn,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = MaterialTheme.shapes.extraLarge,
        border = BulletinButtonDefaults.outlinedButtonBorder(),
        colors = BulletinButtonDefaults.outlinedButtonColors()
    ) {
        Text(
            text = "Back to Sign In",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun SignUpFormContent(
    uiState: ProfileUiState,
    onClearMessages: () -> Unit,
    onCreateAccount: (String, String, String, String) -> Unit,
    onNavigateToSignIn: () -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Text(
        text = "Get started on Bulletin",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Sign up to access your campus marketplace and connect with peers safely.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(28.dp))

    uiState.errorMessage?.let { error ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(12.dp)
        ) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    NameInputRow(
        firstName = firstName,
        onFirstNameChange = {
            firstName = it
            if (uiState.errorMessage != null) onClearMessages()
        },
        lastName = lastName,
        onLastNameChange = {
            lastName = it
            if (uiState.errorMessage != null) onClearMessages()
        }
    )

    Spacer(modifier = Modifier.height(16.dp))

    EmailInputField(
        email = email,
        onEmailChange = {
            email = it
            if (uiState.errorMessage != null) onClearMessages()
        }
    )

    Spacer(modifier = Modifier.height(16.dp))

    PasswordInputField(
        password = password,
        onPasswordChange = {
            password = it
            if (uiState.errorMessage != null) onClearMessages()
        }
    )

    Spacer(modifier = Modifier.height(40.dp))

    Button(
        onClick = { onCreateAccount(firstName, lastName, email, password) },
        enabled = !uiState.isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = BulletinButtonDefaults.buttonColors()
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = "Create account",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 160.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Already have an account?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onNavigateToSignIn) {
            Text(
                text = "Log in",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun NameInputRow(
    firstName: String,
    onFirstNameChange: (String) -> Unit,
    lastName: String,
    onLastNameChange: (String) -> Unit
) {
    val inputFieldColors = BulletinTextFieldDefaults.colors()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            RequiredFieldLabel(text = "First name")
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = firstName,
                onValueChange = onFirstNameChange,
                placeholder = { Text(text = "First name") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = inputFieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            RequiredFieldLabel(text = "Last name")
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = lastName,
                onValueChange = onLastNameChange,
                placeholder = { Text(text = "Last name") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = inputFieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            )
        }
    }
}

@Composable
private fun EmailInputField(
    email: String,
    onEmailChange: (String) -> Unit
) {
    val inputFieldColors = BulletinTextFieldDefaults.colors()

    Column(modifier = Modifier.fillMaxWidth()) {
        RequiredFieldLabel(text = "School email")
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = { Text(text = "you@student.school.edu") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = inputFieldColors,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        )
    }
}

@Composable
private fun PasswordInputField(
    password: String,
    onPasswordChange: (String) -> Unit
) {
    var isPasswordVisible by remember { mutableStateOf(false) }
    val inputFieldColors = BulletinTextFieldDefaults.colors()

    Column(modifier = Modifier.fillMaxWidth()) {
        RequiredFieldLabel(text = "Password")
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = { Text(text = "Create a password") },
            singleLine = true,
            visualTransformation = if (isPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    val icon = if (isPasswordVisible) {
                        Res.drawable.ic_visibility
                    } else {
                        Res.drawable.ic_visibility_off
                    }
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = inputFieldColors,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        )
    }
}

@Composable
private fun RequiredFieldLabel(text: String) {
    Row {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = " *",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.error
        )
    }
}
