package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.SectionHeader
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail

@Composable
internal fun EmailVerificationRecoveryContent(
    uiState: RegistrationUiState,
    email: StudentEmail,
    onRetry: () -> Unit,
    onSignIn: () -> Unit
) {
    BulletinCard {
        Column {
            SectionHeader("Email verified", email.value)
            Text(
                "Your code was accepted, but Bulletin could not finish loading your profile.",
                style = MaterialTheme.typography.bodyMedium
            )
            uiState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            uiState.informationMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            if (uiState.isLoading) CircularProgressIndicator()
            BulletinButton(
                onClick = onRetry,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
                variant = BulletinButtonVariant.Primary
            ) { Text("Retry setup") }
            BulletinButton(
                onClick = onSignIn,
                enabled = !uiState.isLoading,
                variant = BulletinButtonVariant.Text
            ) { Text("Sign in instead") }
        }
    }
}
