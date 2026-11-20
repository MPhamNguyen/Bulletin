package com.jdrms.bulletin.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

internal const val DEFAULT_SELLER_MESSAGE = "Hi, is this available?"

data class MessageSellerQuickActionActions(
    val conversationKey: String,
    val hasExistingConversation: suspend () -> Boolean,
    val onSend: suspend (String) -> Boolean,
    val onSent: () -> Unit,
    val onSeeChat: suspend () -> Unit
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
    modifier: Modifier = Modifier,
    canSend: Boolean = true
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

    MessageSellerQuickActionContent(
        status = status.copy(canSend = canSend),
        modifier = modifier,
        isSending = isSending,
        errorMessage = errorMessage,
        onValueChange = {
            if (conversationExists == false) {
                message = it
                errorMessage = null
            }
        },
        onSend = {
            if (status.isMessageSent) {
                scope.launch { actions.onSeeChat() }
                return@MessageSellerQuickActionContent
            }
            if (!canSend || isSending) return@MessageSellerQuickActionContent
            isSending = true
            errorMessage = null
            scope.launch {
                val sent = actions.onSend(message)
                isSending = false
                if (sent) actions.onSent() else errorMessage = "Unable to send message. Please try again."
            }
        },
    )
}
