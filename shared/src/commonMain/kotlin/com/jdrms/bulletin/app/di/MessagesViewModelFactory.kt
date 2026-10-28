package com.jdrms.bulletin.app.di

import com.jdrms.bulletin.domain.messages.presentation.MessageThreadOperations
import com.jdrms.bulletin.domain.messages.presentation.MessagesViewModel

internal fun MessagesModule.createMessagesViewModel() = MessagesViewModel(
    inboxStateOwner,
    getConversationMessages,
    observeConversationMessages,
    sendMessage,
    reportMessage,
    hideConversation,
    conversationListingPreviewProvider,
    MessageThreadOperations(markConversationRead, editMessage)
)
