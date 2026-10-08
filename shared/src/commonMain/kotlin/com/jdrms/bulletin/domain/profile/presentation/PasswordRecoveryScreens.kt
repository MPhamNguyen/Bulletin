package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import bulletin.shared.generated.resources.Res
import bulletin.shared.generated.resources.ic_visibility
import bulletin.shared.generated.resources.ic_visibility_off
import com.jdrms.bulletin.core.designsystem.BulletinButtonDefaults
import com.jdrms.bulletin.core.designsystem.BulletinTextFieldDefaults
import org.jetbrains.compose.resources.painterResource

@Composable
fun ForgotPasswordScreen(
    errorMessage: String? = null,
    isLoading: Boolean = false,
    onSubmit: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    RecoveryLayout(
        title = "Forgot your password?",
        description = "Enter your email to start a password reset.",
        errorMessage = errorMessage,
        content = {
            RecoveryField(label = "Email", value = email, placeholder = "you@university.edu") {
                email = it
            }
            Spacer(Modifier.height(20.dp))
            RecoveryButton("Continue", isLoading) { onSubmit(email) }
            TextButton(onClick = onBack, colors = BulletinButtonDefaults.textButtonColors()) {
                Text("Back to sign in")
            }
        }
    )
}

@Composable
fun PasswordConfirmationCodeScreen(
    email: String,
    errorMessage: String? = null,
    isLoading: Boolean = false,
    onSubmit: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    var code by remember { mutableStateOf("") }
    RecoveryLayout(
        title = "Confirm your reset",
        description = "Enter the confirmation code sent to $email.",
        errorMessage = errorMessage,
        content = {
            RecoveryField(label = "Confirmation code", value = code, placeholder = "6-digit code") {
                code = it
            }
            Spacer(Modifier.height(20.dp))
            RecoveryButton("Confirm code", isLoading) { onSubmit(code) }
            TextButton(onClick = onBack, colors = BulletinButtonDefaults.textButtonColors()) {
                Text("Back")
            }
        }
    )
}

@Composable
fun ChangePasswordScreen(
    errorMessage: String? = null,
    isLoading: Boolean = false,
    onSubmit: (String, String) -> Unit = { _, _ -> },
    onBack: () -> Unit = {}
) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    RecoveryLayout(
        title = "Choose a new password",
        description = "Use at least 8 characters, then confirm your new password.",
        errorMessage = errorMessage,
        content = {
            RecoveryField(
                label = "New password",
                value = password,
                placeholder = "Enter a new password",
                isPassword = true,
                onValueChange = { password = it }
            )
            Spacer(Modifier.height(16.dp))
            RecoveryField(
                label = "Confirm password",
                value = confirmation,
                placeholder = "Re-enter your password",
                isPassword = true,
                onValueChange = { confirmation = it }
            )
            Spacer(Modifier.height(20.dp))
            RecoveryButton("Change password", isLoading) { onSubmit(password, confirmation) }
            TextButton(onClick = onBack, colors = BulletinButtonDefaults.textButtonColors()) {
                Text("Back")
            }
        }
    )
}

@Composable
private fun RecoveryLayout(
    title: String,
    description: String,
    errorMessage: String?,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 32.dp)
            .widthIn(max = 480.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        errorMessage?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))
        }
        content()
    }
}

@Composable
private fun RecoveryField(
    label: String,
    value: String,
    placeholder: String,
    isPassword: Boolean = false,
    onValueChange: (String) -> Unit
) {
    var isPasswordVisible by remember { mutableStateOf(false) }

    Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        singleLine = true,
        visualTransformation = if (isPassword && !isPasswordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    val icon = if (isPasswordVisible) Res.drawable.ic_visibility else Res.drawable.ic_visibility_off
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            null
        },
        shape = MaterialTheme.shapes.medium,
        colors = BulletinTextFieldDefaults.colors()
    )
}

@Composable
private fun RecoveryButton(label: String, isLoading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = BulletinButtonDefaults.buttonColors()
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.height(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}
