package com.jdrms.bulletin.domain.inbox.presentation

import com.jdrms.bulletin.domain.inbox.domain.model.Conversation
import com.jdrms.bulletin.domain.inbox.domain.model.ConversationId
import com.jdrms.bulletin.domain.inbox.domain.model.Message

data class MessagingUiState(
    val conversations: List<Conversation> = emptyList(),
    val selectedConversationId: ConversationId? = null,
    val currentMessages: List<Message> = emptyList(),
    val messageInput: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
