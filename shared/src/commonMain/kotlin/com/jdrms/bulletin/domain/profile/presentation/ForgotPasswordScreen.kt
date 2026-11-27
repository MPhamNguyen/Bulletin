package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bulletin.shared.generated.resources.Res
import bulletin.shared.generated.resources.ic_arrow_back
import bulletin.shared.generated.resources.ic_graduation_cap
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinShapes
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import org.jetbrains.compose.resources.painterResource

@Composable
fun ForgotPasswordScreen(
    errorMessage: String? = null,
    isLoading: Boolean = false,
    onSubmit: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = BulletinSpacing.xl, vertical = BulletinSpacing.l),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ForgotPasswordHeader()
            errorMessage?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(BulletinSpacing.m))
            }
            Column(Modifier.fillMaxWidth()) {
                RecoveryField(
                    label = "Email",
                    value = email,
                    placeholder = "you@university.edu",
                    keyboardType = KeyboardType.Email
                ) { email = it }
                Spacer(Modifier.height(BulletinSpacing.xl))
                RecoveryButton("Send reset link", isLoading, enabled = email.isNotBlank()) { onSubmit(email) }
            }
            Spacer(Modifier.height(BulletinSpacing.l))
            BulletinButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                variant = BulletinButtonVariant.Ghost
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_arrow_back),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(BulletinSpacing.s))
                Text("Back to sign in", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ForgotPasswordHeader() {
    Spacer(Modifier.height(BulletinSpacing.xl))
    Box(
        modifier = Modifier.size(80.dp).background(MaterialTheme.colorScheme.primary, BulletinShapes.pill),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_graduation_cap),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(40.dp)
        )
    }
    Spacer(Modifier.height(BulletinSpacing.xl))
    Text(
        "Forgot password?",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(BulletinSpacing.s))
    Text(
        "Enter your university email to receive reset instructions",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(max = 320.dp)
    )
    Spacer(Modifier.height(28.dp))
}
