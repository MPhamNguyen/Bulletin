package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import bulletin.shared.generated.resources.Res
import bulletin.shared.generated.resources.ic_graduation_cap
import bulletin.shared.generated.resources.ic_visibility
import bulletin.shared.generated.resources.ic_visibility_off
import com.jdrms.bulletin.core.designsystem.theme.BulletinTextFieldDefaults
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun SignInEmailField(email: String, onEmailChange: (String) -> Unit, onClearMessages: () -> Unit) {
    SignInFieldLabel(text = "Email")
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
        value = email,
        onValueChange = {
            onEmailChange(it)
            onClearMessages()
        },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("you@university.edu") },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        colors = BulletinTextFieldDefaults.colors()
    )

    Spacer(modifier = Modifier.height(18.dp))
}

@Composable
internal fun SignInPasswordField(password: String, onPasswordChange: (String) -> Unit, onClearMessages: () -> Unit) {
    var isPasswordVisible by remember { mutableStateOf(false) }
    SignInFieldLabel(text = "Password")
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
        value = password,
        onValueChange = {
            onPasswordChange(it)
            onClearMessages()
        },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Enter your password") },
        singleLine = true,
        visualTransformation = if (isPasswordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            PasswordVisibilityButton(isPasswordVisible) { isPasswordVisible = !isPasswordVisible }
        },
        shape = MaterialTheme.shapes.medium,
        colors = BulletinTextFieldDefaults.colors()
    )
}

@Composable
internal fun BulletinMark() {
    Box(
        modifier = Modifier
            .size(96.dp)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_graduation_cap),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(54.dp)
        )
    }
}

@Composable
private fun SignInFieldLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun PasswordVisibilityButton(visible: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(if (visible) Res.drawable.ic_visibility else Res.drawable.ic_visibility_off),
            contentDescription = if (visible) "Hide password" else "Show password",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
