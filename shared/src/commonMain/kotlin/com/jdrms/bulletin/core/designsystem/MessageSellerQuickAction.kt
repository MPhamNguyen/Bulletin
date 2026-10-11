package com.jdrms.bulletin.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private const val DEFAULT_SELLER_MESSAGE = "Hi, is this available?"

data class MessageSellerQuickActionActions(
    val conversationKey: String,
    val hasExistingConversation: suspend () -> Boolean,
    val onSend: suspend (String) -> Boolean,
    val onSent: () -> Unit,
    val onSeeChat: suspend () -> Unit
)

private data class MessageSellerQuickActionStatus(
    val fieldText: String,
    val buttonText: String,
    val isEditable: Boolean,
    val isMessageSent: Boolean
)

private fun messageSellerStatus(conversationExists: Boolean?, message: String): MessageSellerQuickActionStatus =
    when (conversationExists) {
        true -> MessageSellerQuickActionStatus("Message Sent", "See Chat", false, true)
        false -> MessageSellerQuickActionStatus(message, "Send", true, false)
        null -> MessageSellerQuickActionStatus("Checking...", "Checking...", false, false)
    }

@Composable
fun MessageSellerQuickAction(
    actions: MessageSellerQuickActionActions,
    modifier: Modifier = Modifier
) {
    var message by remember { mutableStateOf(DEFAULT_SELLER_MESSAGE) }
    var isSending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var conversationExists by remember(actions.conversationKey) { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()
    val status = messageSellerStatus(conversationExists, message)

    LaunchedEffect(actions.conversationKey) {
        conversationExists = actions.hasExistingConversation()
    }

    androidx.compose.foundation.layout.Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Message Seller", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = status.fieldText,
                onValueChange = {
                    if (conversationExists == false) {
                        message = it
                        errorMessage = null
                    }
                },
                modifier = Modifier.weight(1f),
                minLines = 1,
                maxLines = 3,
                placeholder = { Text(DEFAULT_SELLER_MESSAGE) },
                readOnly = !status.isEditable,
                leadingIcon = if (status.isMessageSent) {
                    { androidx.compose.material3.Icon(Icons.Filled.Check, contentDescription = "Message sent") }
                } else {
                    null
                },
                textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                    color = if (!status.isEditable) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                ),
                colors = BulletinTextFieldDefaults.colors()
            )
            Button(
                onClick = {
                    if (status.isMessageSent) {
                        scope.launch { actions.onSeeChat() }
                        return@Button
                    }
                    if (isSending) return@Button
                    isSending = true
                    errorMessage = null
                    scope.launch {
                        val sent = actions.onSend(message)
                        isSending = false
                        if (sent) actions.onSent() else errorMessage = "Unable to send message. Please try again."
                    }
                },
                enabled = !isSending && (status.isMessageSent || (status.isEditable && message.isNotBlank())),
                colors = BulletinButtonDefaults.buttonColors()
            ) {
                if (isSending) {
                    CircularProgressIndicator(modifier = Modifier, strokeWidth = 2.dp)
                } else {
                    Text(
                        status.buttonText
                    )
                }
            }
        }
        errorMessage?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
