package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
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
fun ProfileScreen(viewModel: ProfileViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ProfileTopBar(
            title = if (uiState.isAccountCreated) "Profile" else "Sign Up",
            onBackClick = { viewModel.clearMessages() }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProfileLogoHeader()

            val createdProfile = uiState.profile
            if (uiState.isAccountCreated && createdProfile != null) {
                SignUpSuccessContent(
                    profile = createdProfile,
                    onRegisterAnother = { viewModel.resetRegistration() }
                )
            } else {
                SignUpFormContent(
                    uiState = uiState,
                    onClearMessages = { viewModel.clearMessages() },
                    onCreateAccount = { first, last, mail, pass ->
                        viewModel.createAccount(first, last, mail, pass)
                    },
                    onLogin = { mail, pass ->
                        viewModel.login(mail, pass)
                    }
                )
            }
        }
    }
}

@Composable
private fun ProfileTopBar(
    title: String,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                painter = painterResource(Res.drawable.ic_arrow_back),
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .padding(end = 48.dp)
        )
    }
}

@Composable
private fun ProfileLogoHeader() {
    Spacer(modifier = Modifier.height(16.dp))

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

    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun SignUpSuccessContent(
    profile: StudentProfile,
    onRegisterAnother: () -> Unit
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
            text = "Account created successfully!",
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

    OutlinedButton(
        onClick = onRegisterAnother,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = MaterialTheme.shapes.extraLarge,
        border = BulletinButtonDefaults.outlinedButtonBorder(),
        colors = BulletinButtonDefaults.outlinedButtonColors()
    ) {
        Text(
            text = "Register another account",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun SignUpFormContent(
    uiState: ProfileUiState,
    onClearMessages: () -> Unit,
    onCreateAccount: (String, String, String, String) -> Unit,
    onLogin: (String, String) -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Text(
        text = "Get started on Bulletin",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(10.dp))

    Text(
        text = "Sign up to access your campus marketplace and connect with peers safely.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(24.dp))

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

    Spacer(modifier = Modifier.height(16.dp))

    LegalDisclosureText()

    Spacer(modifier = Modifier.height(20.dp))

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
                text = "Create Account",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedButton(
        onClick = {
            if (email.isNotBlank() && password.isNotBlank()) {
                onLogin(email, password)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = MaterialTheme.shapes.extraLarge,
        border = BulletinButtonDefaults.outlinedButtonBorder(),
        colors = BulletinButtonDefaults.outlinedButtonColors()
    ) {
        Text(
            text = "I already have an account",
            style = MaterialTheme.typography.titleMedium
        )
    }

    Spacer(modifier = Modifier.height(16.dp))
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
            RequiredFieldLabel(text = "First Name")
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = firstName,
                onValueChange = onFirstNameChange,
                placeholder = { Text(text = "First Name") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = inputFieldColors,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            RequiredFieldLabel(text = "Last Name")
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = lastName,
                onValueChange = onLastNameChange,
                placeholder = { Text(text = "Last Name") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = inputFieldColors,
                modifier = Modifier.fillMaxWidth()
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
        RequiredFieldLabel(text = "Email")
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = { Text(text = "Email") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = inputFieldColors,
            modifier = Modifier.fillMaxWidth()
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
            placeholder = { Text(text = "Password") },
            singleLine = true,
            visualTransformation = if (isPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    val icon = if (isPasswordVisible) {
                        Res.drawable.ic_visibility_off
                    } else {
                        Res.drawable.ic_visibility
                    }
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            shape = MaterialTheme.shapes.medium,
            colors = inputFieldColors,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LegalDisclosureText() {
    val legalAnnotatedText = buildAnnotatedString {
        append("By tapping Create Account, you agree to create an account and to Bulletin's ")
        withStyle(
            SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                textDecoration = TextDecoration.Underline
            )
        ) {
            append("Terms")
        }
        append(", ")
        withStyle(
            SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                textDecoration = TextDecoration.Underline
            )
        ) {
            append("Privacy Policy")
        }
        append(" and ")
        withStyle(
            SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                textDecoration = TextDecoration.Underline
            )
        ) {
            append("Cookies Policy")
        }
        append(".")
    }

    Text(
        text = legalAnnotatedText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
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
