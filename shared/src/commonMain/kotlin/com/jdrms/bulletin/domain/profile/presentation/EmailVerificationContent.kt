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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.jdrms.bulletin.core.designsystem.BulletinButtonDefaults
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.SectionHeader
import kotlinx.coroutines.delay

private const val CODE_LENGTH = 6
private const val RESEND_FEEDBACK_MS = 3_000L

@Composable
internal fun EmailVerificationContent(
    uiState: ProfileUiState,
    onVerify: (String) -> Unit,
    onResend: () -> Unit,
    onChangeEmail: () -> Unit
) {
    var code by remember(uiState.pendingRegistration) { mutableStateOf("") }
    var resendTick by remember { mutableStateOf(0) }
    val resendSent = resendTick > 0
    val colors = MaterialTheme.colorScheme
    val canVerify = code.length == CODE_LENGTH && !uiState.isLoading

    LaunchedEffect(resendTick) {
        if (resendTick > 0) {
            delay(RESEND_FEEDBACK_MS)
            resendTick = 0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
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
                .padding(top = 48.dp),
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
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.01).em,
                color = colors.onBackground,
                modifier = Modifier.padding(top = 24.dp)
            )
            Text(
                text = "We sent a 6-digit code to",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp)
            )
            uiState.pendingRegistration?.email?.value?.let { email ->
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Text(
                text = "Can’t find it? Check your spam folder.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )

            val codeTextStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 26.sp,
                letterSpacing = 0.5.em,
                textAlign = TextAlign.Center,
                color = colors.onBackground
            )
            OutlinedTextField(
                value = code,
                onValueChange = { input -> code = input.filter(Char::isDigit).take(CODE_LENGTH) },
                placeholder = {
                    Text(
                        "······",
                        style = codeTextStyle.copy(color = colors.onSurfaceVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                textStyle = codeTextStyle,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                enabled = !uiState.isLoading,
                isError = uiState.errorMessage != null,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surfaceVariant,
                    unfocusedContainerColor = colors.surfaceVariant,
                    disabledContainerColor = colors.surfaceVariant,
                    errorContainerColor = colors.surfaceVariant,
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent,
                    errorBorderColor = colors.error,
                    cursorColor = colors.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(20.dp)
            ) {
                val message = uiState.errorMessage ?: uiState.successMessage
                if (message != null) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (uiState.errorMessage != null) colors.error else colors.onSurfaceVariant
                    )
                }
            }
            Button(
                onClick = { onVerify(code) },
                enabled = canVerify,
                shape = CircleShape,
                colors = BulletinButtonDefaults.buttonColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(52.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = colors.onPrimary)
                } else {
                    Text("Verify email", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Column(
                modifier = Modifier.padding(top = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(
                    onClick = {
                        onResend()
                        resendTick++
                    },
                    enabled = !uiState.isLoading && !resendSent,
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(
                        text = if (resendSent) {
                            "Verification email sent"
                        } else {
                            "Didn’t get a code? Resend verification email"
                        },
                        fontWeight = FontWeight.Medium
                    )
                }
                TextButton(
                    onClick = onChangeEmail,
                    enabled = !uiState.isLoading,
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("Use a different email", color = colors.onSurfaceVariant, fontWeight = FontWeight.Medium)
                }
            }
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
