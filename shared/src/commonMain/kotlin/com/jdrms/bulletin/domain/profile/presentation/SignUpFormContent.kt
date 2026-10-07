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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.jdrms.bulletin.core.designsystem.BulletinButtonDefaults
import com.jdrms.bulletin.core.designsystem.BulletinTextFieldDefaults
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun SignUpFormContent(
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
                .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.medium)
                .padding(12.dp)
        ) {
            Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
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
        modifier = Modifier.fillMaxWidth().height(52.dp),
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
            Text("Create account", style = MaterialTheme.typography.titleMedium)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 160.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Already have an account?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onNavigateToSignIn) {
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
    val inputFieldColors = BulletinTextFieldDefaults.colors()
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        NameInputField("First name", firstName, onFirstNameChange, inputFieldColors)
        NameInputField("Last name", lastName, onLastNameChange, inputFieldColors)
    }
}

@Composable
private fun RowScope.NameInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    colors: androidx.compose.material3.TextFieldColors
) {
    Column(modifier = Modifier.weight(1f)) {
        RequiredFieldLabel(label)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(label) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = colors,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        )
    }
}

@Composable
private fun EmailInputField(email: String, onEmailChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        RequiredFieldLabel("School email")
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = { Text("you@student.school.edu") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = BulletinTextFieldDefaults.colors(),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        )
    }
}

@Composable
private fun PasswordInputField(password: String, onPasswordChange: (String) -> Unit) {
    var isPasswordVisible by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        RequiredFieldLabel("Password")
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = { Text("Create a password") },
            singleLine = true,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    val icon = if (isPasswordVisible) Res.drawable.ic_visibility else Res.drawable.ic_visibility_off
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = BulletinTextFieldDefaults.colors(),
            modifier = Modifier.fillMaxWidth().height(52.dp)
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
