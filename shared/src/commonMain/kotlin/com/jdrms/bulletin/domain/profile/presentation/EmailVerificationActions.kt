package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

@Composable
internal fun EmailVerificationActions(
    isLoading: Boolean,
    secondsLeft: Int,
    onResend: () -> Unit,
    onSignIn: () -> Unit,
    onChangeEmail: () -> Unit
) {
    Column(
        modifier = Modifier.padding(top = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BulletinSpacing.xs)
    ) {
        BulletinButton(
            onClick = onResend,
            enabled = !isLoading && secondsLeft <= 0,
            modifier = Modifier,
            variant = BulletinButtonVariant.Text
        ) {
            Text(
                text = if (secondsLeft > 0) {
                    "Didn’t get a code? Resend in ${secondsLeft}s"
                } else {
                    "Didn’t get a code? Resend verification email"
                },
                fontWeight = FontWeight.Medium
            )
        }
        BulletinButton(
            onClick = onSignIn,
            enabled = !isLoading,
            variant = BulletinButtonVariant.Text
        ) {
            Text("Already have an account? Sign in", fontWeight = FontWeight.Medium)
        }
        BulletinButton(
            onClick = onChangeEmail,
            enabled = !isLoading,
            modifier = Modifier,
            variant = BulletinButtonVariant.Text
        ) {
            Text(
                "Use a different email",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
