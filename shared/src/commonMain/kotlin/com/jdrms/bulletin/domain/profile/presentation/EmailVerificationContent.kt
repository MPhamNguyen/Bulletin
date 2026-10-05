package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.input.KeyboardType
import com.jdrms.bulletin.core.designsystem.BulletinButtonDefaults
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.BulletinTextFieldDefaults
import com.jdrms.bulletin.core.designsystem.SectionHeader

@Composable
internal fun EmailVerificationContent(
    uiState: ProfileUiState,
    onVerify: (String) -> Unit,
    onResend: () -> Unit,
    onChangeEmail: () -> Unit
) {
    var code by remember(uiState.pendingRegistration) { mutableStateOf("") }
    BulletinCard {
        Column {
            SectionHeader("Verify your email", uiState.pendingRegistration?.email?.value)
            Text(
                "Check your inbox and spam folder for your code. Enter it below to finish creating your account.",
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("6-digit verification code") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                enabled = !uiState.isLoading,
                shape = MaterialTheme.shapes.medium,
                colors = BulletinTextFieldDefaults.colors(),
                modifier = Modifier.fillMaxWidth()
            )
            uiState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            uiState.successMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            if (uiState.isLoading) CircularProgressIndicator()
            Button(
                onClick = { onVerify(code) },
                enabled = !uiState.isLoading,
                colors = BulletinButtonDefaults.buttonColors(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Verify email") }
            TextButton(onClick = onResend, enabled = !uiState.isLoading) { Text("Resend code") }
            TextButton(onClick = onChangeEmail, enabled = !uiState.isLoading) { Text("Use a different email") }
        }
    }
}

@Composable
internal fun EmailVerificationRecoveryContent(
    uiState: ProfileUiState,
    onRetry: () -> Unit,
    onSignIn: () -> Unit
) {
    BulletinCard {
        Column {
            SectionHeader("Email verified", uiState.verifiedEmailAwaitingProfile?.value)
            Text(
                "Your code was accepted, but Bulletin could not finish loading your profile.",
                style = MaterialTheme.typography.bodyMedium
            )
            uiState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            uiState.successMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            if (uiState.isLoading) CircularProgressIndicator()
            Button(
                onClick = onRetry,
                enabled = !uiState.isLoading,
                colors = BulletinButtonDefaults.buttonColors(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Retry setup") }
            TextButton(onClick = onSignIn, enabled = !uiState.isLoading) { Text("Sign in instead") }
        }
    }
}
