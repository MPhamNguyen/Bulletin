package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import bulletin.shared.generated.resources.Res
import bulletin.shared.generated.resources.ic_visibility
import bulletin.shared.generated.resources.ic_visibility_off
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.BulletinButtonSize
import com.jdrms.bulletin.core.designsystem.BulletinFieldDecoration
import com.jdrms.bulletin.core.designsystem.BulletinFieldOptions
import com.jdrms.bulletin.core.designsystem.BulletinTextField
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun SignUpFormContent(
    uiState: RegistrationUiState,
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
    Spacer(modifier = Modifier.height(BulletinSpacing.s))
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
                .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.medium)
                .padding(BulletinSpacing.m)
        ) {
            Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
        }
        Spacer(modifier = Modifier.height(BulletinSpacing.l))
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
    Spacer(modifier = Modifier.height(BulletinSpacing.l))
    EmailInputField(
        email = email,
        onEmailChange = {
            email = it
            if (uiState.errorMessage != null) onClearMessages()
        }
    )
    Spacer(modifier = Modifier.height(BulletinSpacing.l))
    PasswordInputField(
        password = password,
        onPasswordChange = {
            password = it
            if (uiState.errorMessage != null) onClearMessages()
        }
    )
    Spacer(modifier = Modifier.height(40.dp))

    BulletinButton(
        onClick = { onCreateAccount(firstName, lastName, email, password) },
        enabled = !uiState.isLoading,
        modifier = Modifier.fillMaxWidth(),
        variant = BulletinButtonVariant.Primary,
        size = BulletinButtonSize.Large
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text("Create account", style = MaterialTheme.typography.titleMedium)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 160.dp, bottom = BulletinSpacing.xl),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Already have an account?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        BulletinButton(onClick = onNavigateToSignIn, variant = BulletinButtonVariant.Text) {
            Text("Log in", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
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
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.m)) {
        NameInputField("First name", firstName, onFirstNameChange)
        NameInputField("Last name", lastName, onLastNameChange)
    }
}

@Composable
private fun RowScope.NameInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.weight(1f)) {
        RequiredFieldLabel(label)
        Spacer(modifier = Modifier.height(6.dp))
        BulletinTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            decoration = BulletinFieldDecoration(
                placeholder = { Text(label) }
            )
        )
    }
}

@Composable
private fun EmailInputField(email: String, onEmailChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        RequiredFieldLabel("School email")
        Spacer(modifier = Modifier.height(6.dp))
        BulletinTextField(
            value = email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            decoration = BulletinFieldDecoration(
                placeholder = { Text("you@student.school.edu") }
            )
        )
    }
}

@Composable
private fun PasswordInputField(password: String, onPasswordChange: (String) -> Unit) {
    var isPasswordVisible by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        RequiredFieldLabel("Password")
        Spacer(modifier = Modifier.height(6.dp))
        BulletinTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            decoration = BulletinFieldDecoration(
                placeholder = { Text("Create a password") },
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        val icon = if (isPasswordVisible) Res.drawable.ic_visibility else Res.drawable.ic_visibility_off
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            ),
            options = BulletinFieldOptions(

                visualTransformation = if (isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                }
            )
        )
    }
}

@Composable
private fun RequiredFieldLabel(text: String) {
    Row {
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(" *", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
    }
}
