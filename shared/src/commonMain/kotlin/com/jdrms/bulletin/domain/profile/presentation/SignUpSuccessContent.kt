package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinButtonDefaults
import com.jdrms.bulletin.core.designsystem.BulletinExtras
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile

@Composable
internal fun SignUpSuccessContent(
    profile: StudentProfile,
    successMessage: String?,
    onContinueToApp: () -> Unit,
    onNavigateToSignIn: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(BulletinExtras.colors.successContainer, MaterialTheme.shapes.medium)
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
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = BulletinButtonDefaults.buttonColors()
    ) {
        Text("Continue to Bulletin", style = MaterialTheme.typography.titleMedium)
    }
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedButton(
        onClick = onNavigateToSignIn,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = MaterialTheme.shapes.extraLarge,
        border = BulletinButtonDefaults.outlinedButtonBorder(),
        colors = BulletinButtonDefaults.outlinedButtonColors()
    ) {
        Text("Back to Sign In", style = MaterialTheme.typography.titleMedium)
    }
}
