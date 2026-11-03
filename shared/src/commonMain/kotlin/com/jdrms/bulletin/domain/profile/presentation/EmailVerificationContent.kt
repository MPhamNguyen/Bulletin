package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.BulletinButtonSize
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.BulletinFieldDecoration
import com.jdrms.bulletin.core.designsystem.BulletinFieldOptions
import com.jdrms.bulletin.core.designsystem.BulletinTextField
import com.jdrms.bulletin.core.designsystem.SectionHeader
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinMotion
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.core.designsystem.theme.BulletinTextStyles
import com.jdrms.bulletin.domain.profile.domain.model.PendingRegistration
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import kotlinx.coroutines.delay

private const val CODE_LENGTH = 6
private const val RESEND_COOLDOWN_SECONDS = 30

@Composable
internal fun EmailVerificationContent(
    uiState: RegistrationUiState,
    pending: PendingRegistration,
    onVerify: (String) -> Unit,
    onResend: () -> Unit,
    onChangeEmail: () -> Unit
) {
    var code by remember(pending) { mutableStateOf("") }
    var secondsLeft by remember(pending) { mutableStateOf(RESEND_COOLDOWN_SECONDS) }
    var cooldownRun by remember { mutableStateOf(0) }
    val colors = MaterialTheme.colorScheme
    val canVerify = code.length == CODE_LENGTH && !uiState.isLoading

    LaunchedEffect(pending, cooldownRun) {
        secondsLeft = RESEND_COOLDOWN_SECONDS
        while (secondsLeft > 0) {
            delay(BulletinMotion.TICK_MILLIS)
            secondsLeft--
        }
    }
    val resendLocked = secondsLeft > 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = BulletinSpacing.xl)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            IconButton(
                onClick = onChangeEmail,
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(44.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.onBackground)
            }
            Text(
                text = "Sign up",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onBackground,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = BulletinSpacing.control),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Email, null, tint = colors.onPrimary, modifier = Modifier.size(34.dp))
            }
            Text(
                text = "Verify your email",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onBackground,
                modifier = Modifier.padding(top = BulletinSpacing.xl)
            )
            Text(
                text = "We sent a 6-digit code to",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                text = pending.email.value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = "Can’t find it? Check your spam folder.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = BulletinSpacing.m)
            )

            val codeTextStyle = BulletinTextStyles.otpInput.copy(color = colors.onBackground)
            BulletinTextField(
                value = code,
                onValueChange = { input -> code = input.filter(Char::isDigit).take(CODE_LENGTH) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = BulletinSpacing.l),
                isError = uiState.errorMessage != null,
                decoration = BulletinFieldDecoration(
                    placeholder = {
                        Text(
                            "······",
                            style = codeTextStyle.copy(color = colors.onSurfaceVariant.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                ),
                options = BulletinFieldOptions(
                    enabled = !uiState.isLoading,

                    textStyle = codeTextStyle,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = BulletinSpacing.s)
                    .height(20.dp)
            ) {
                val message = uiState.errorMessage ?: uiState.informationMessage
                if (message != null) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (uiState.errorMessage != null) colors.error else colors.onSurfaceVariant
                    )
                }
            }
            BulletinButton(
                onClick = { onVerify(code) },
                enabled = canVerify,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = BulletinSpacing.m),
                variant = BulletinButtonVariant.Primary,
                size = BulletinButtonSize.Large
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = colors.onPrimary)
                } else {
                    Text("Verify email", style = MaterialTheme.typography.titleMedium)
                }
            }
            Column(
                modifier = Modifier.padding(top = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(BulletinSpacing.xs)
            ) {
                BulletinButton(
                    onClick = {
                        onResend()
                        cooldownRun++
                    },
                    enabled = !uiState.isLoading && !resendLocked,
                    modifier = Modifier,
                    variant = BulletinButtonVariant.Text
                ) {
                    Text(
                        text = if (resendLocked) {
                            "Didn’t get a code? Resend in ${secondsLeft}s"
                        } else {
                            "Didn’t get a code? Resend verification email"
                        },
                        fontWeight = FontWeight.Medium
                    )
                }
                BulletinButton(
                    onClick = onChangeEmail,
                    enabled = !uiState.isLoading,
                    modifier = Modifier,
                    variant = BulletinButtonVariant.Text
                ) {
                    Text("Use a different email", color = colors.onSurfaceVariant, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

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
