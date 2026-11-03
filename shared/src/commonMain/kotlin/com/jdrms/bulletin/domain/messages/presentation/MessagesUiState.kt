package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.application.ConversationListingPreview
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageCursor
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.reporting.domain.model.ReportReason

data class MessagesUiState(
    val viewerId: SenderId? = null,
    val conversations: List<Conversation> = emptyList(),
    val listingPreviews: Map<String, ConversationListingPreview?> = emptyMap(),
    val selectedConversationId: ConversationId? = null,
    val isConversationOpen: Boolean = false,
    val currentMessages: List<Message> = emptyList(),
    val revealedReportedMessageIds: Set<MessageId> = emptySet(),
    val olderCursor: MessageCursor? = null,
    val isLoadingOlder: Boolean = false,
    val connectionState: MessageConnectionState = MessageConnectionState.CONNECTING,
    val messageInput: String = "",
    val editState: MessageEditState = MessageEditState.Composing,
    val editError: String? = null,
    val isLoading: Boolean = false,
    val isLoadingMessages: Boolean = false,
    val isSending: Boolean = false,
    val isReporting: Boolean = false,
    val messageReportForm: MessageReportFormState? = null,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
    val block: ConversationBlockUiState = ConversationBlockUiState()
)

data class MessageReportFormState(
    val messageId: MessageId,
    val selectedReason: ReportReason? = null,
    val description: String = "",
    val isSubmitted: Boolean = false,
    val errorMessage: String? = null
)

internal fun MessagesUiState.clearMessageLoadingForMissingConversation(
    conversationId: ConversationId
): MessagesUiState =
    if (selectedConversationId != null && selectedConversationId != conversationId) {
        this
    } else {
        copy(isLoadingMessages = false)
    }

enum class MessageConnectionState { CONNECTING, CONNECTED, DISCONNECTED }
