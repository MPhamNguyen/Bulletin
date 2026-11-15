package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.domain.messages.application.EditMessage
import com.jdrms.bulletin.domain.messages.application.GetConversationMessages
import com.jdrms.bulletin.domain.messages.application.HideConversation
import com.jdrms.bulletin.domain.messages.application.ObserveConversationMessages
import com.jdrms.bulletin.domain.messages.application.ReportMessage
import com.jdrms.bulletin.domain.messages.application.SendMessage
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesObserver
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import com.jdrms.bulletin.domain.messages.presentation.MessageThreadOperations
import com.jdrms.bulletin.domain.messages.presentation.MessagesViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.emptyFlow

internal val secondDraftConversationId = ConversationId("conversation-b")

internal fun draftRepository() = messagesRepository(
    conversations = listOf(conversation(), conversation(secondDraftConversationId))
)

internal fun draftViewModel(
    repository: MessagesRepository,
    provider: MutableMessageSenderProvider,
    dispatcher: CoroutineDispatcher
): MessagesViewModel = MessagesViewModel(
    testInboxOwner(repository, provider, dispatcher),
    GetConversationMessages(repository, provider),
    ObserveConversationMessages(repository as? MessagesObserver ?: MessagesObserver { _, _ -> emptyFlow() }, provider),
    SendMessage(repository, provider, nextMessageId = { MessageId("sent-draft") }, nowMillis = { 123L }),
    ReportMessage(repository, provider),
    HideConversation(repository, provider),
    threadOperations = MessageThreadOperations(edit = EditMessage(repository, provider))
).also { it.onDestinationActiveChanged(true) }
