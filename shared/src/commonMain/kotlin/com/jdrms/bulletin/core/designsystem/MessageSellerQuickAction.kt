package com.jdrms.bulletin.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
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
        verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)
    ) {
        Text("Message Seller", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BulletinTextField(
                value = status.fieldText,
                onValueChange = {
                    if (conversationExists == false) {
                        message = it
                        errorMessage = null
                    }
                },
                modifier = Modifier.weight(1f),
                decoration = BulletinFieldDecoration(
                    placeholder = { Text(DEFAULT_SELLER_MESSAGE) },
                    leadingIcon = if (status.isMessageSent) {
                        { androidx.compose.material3.Icon(Icons.Filled.Check, contentDescription = "Message sent") }
                    } else {
                        null
                    }
                ),
                options = BulletinFieldOptions(
                    readOnly = !status.isEditable,
                    minLines = 1,
                    maxLines = 3,
                    textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                        color = if (!status.isEditable) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                )
            )
            BulletinButton(
                onClick = {
                    if (status.isMessageSent) {
                        scope.launch { actions.onSeeChat() }
                        return@BulletinButton
                    }
                    if (isSending) return@BulletinButton
                    isSending = true
                    errorMessage = null
                    scope.launch {
                        val sent = actions.onSend(message)
                        isSending = false
                        if (sent) actions.onSent() else errorMessage = "Unable to send message. Please try again."
                    }
                },
                enabled = !isSending && (status.isMessageSent || (status.isEditable && message.isNotBlank())),
                variant = BulletinButtonVariant.Primary
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
