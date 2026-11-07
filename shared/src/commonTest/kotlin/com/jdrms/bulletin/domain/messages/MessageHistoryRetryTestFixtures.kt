package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageCursor
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.MessagePage
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import kotlinx.coroutines.CoroutineDispatcher

internal data class HistoryPageCall(
    val viewerId: SenderId,
    val conversationId: ConversationId,
    val before: MessageCursor?,
    val limit: Int
)

internal class MessageHistoryRetryFixture(dispatcher: CoroutineDispatcher, messages: List<Message> = emptyList()) {
    val provider = MutableMessageSenderProvider()
    private val backing = messagesRepository(listOf(conversation(), conversation(secondDraftConversationId)), messages)
    val calls = mutableListOf<HistoryPageCall>()
    var inboxReads = 0
        private set
    var readPage: suspend (HistoryPageCall) -> Result<MessagePage> = ::storedPage
    private val repository = object : MessagesRepository by backing {
        override suspend fun getConversations(userId: SenderId): Result<List<Conversation>> {
            inboxReads++
            return backing.getConversations(userId)
        }

        override suspend fun getMessagePage(
            userId: SenderId,
            conversationId: ConversationId,
            before: MessageCursor?,
            limit: Int
        ): Result<MessagePage> {
            val call = HistoryPageCall(userId, conversationId, before, limit)
            calls += call
            return readPage(call)
        }
    }
    val viewModel = draftViewModel(repository, provider, dispatcher)

    suspend fun storedPage(call: HistoryPageCall): Result<MessagePage> =
        backing.getMessagePage(call.viewerId, call.conversationId, call.before, call.limit)
}

internal fun historyMessages(count: Int = 35): List<Message> = (1..count).map { index ->
    message(id = MessageId("history-$index")).copy(timestampMillis = index.toLong())
}

internal fun historyFailure(): Result.Error = Result.Error(IllegalStateException("Private transport detail"))
