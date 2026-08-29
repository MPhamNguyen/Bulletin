package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.Message

data class MessagesUiState(
    val conversations: List<Conversation> = emptyList(),
    val selectedConversationId: ConversationId? = null,
    val currentMessages: List<Message> = emptyList(),
    val messageInput: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
