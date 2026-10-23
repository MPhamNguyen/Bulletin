package com.jdrms.bulletin.domain.home.presentation.quickmessage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
internal fun MessageComposer(
    message: String,
    editable: Boolean,
    sending: Boolean,
    arrowProgress: State<Float>,
    focusRequester: FocusRequester,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val canSend = editable && message.isNotBlank()
    Surface(
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(start = 18.dp, end = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = message,
                onValueChange = onMessageChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .semantics { contentDescription = "Message to seller" },
                enabled = editable,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(QuickMessageTokens.teal),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                decorationBox = { inner -> MessageInputDecoration(message, inner) }
            )
            SendButton(canSend, sending, arrowProgress, onSend)
        }
    }
}

@Composable
private fun MessageInputDecoration(message: String, inner: @Composable () -> Unit) {
    Box(contentAlignment = Alignment.CenterStart) {
        if (message.isEmpty()) {
            Text(
                "Write a message…",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        inner()
    }
}

@Composable
private fun SendButton(
    enabled: Boolean,
    sending: Boolean,
    arrowProgress: State<Float>,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = if (!enabled && !sending) 0.3f else 1f),
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
            .padding(start = 8.dp)
            .size(44.dp)
            .semantics {
                contentDescription = "Send message"
                role = Role.Button
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            SendArrow(
                Modifier.graphicsLayer {
                    translationY = arrowProgress.value * -26.dp.toPx()
                    alpha = 1f - arrowProgress.value
                }
            )
            if (sending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp).graphicsLayer { alpha = arrowProgress.value },
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
