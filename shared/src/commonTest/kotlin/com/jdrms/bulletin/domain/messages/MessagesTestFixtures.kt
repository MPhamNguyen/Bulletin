package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.CurrentMessageSenderProvider
import com.jdrms.bulletin.domain.messages.application.MessageSender
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.infrastructure.mapper.MessagesMapper
import com.jdrms.bulletin.domain.messages.infrastructure.repository.InMemoryMessagesRepository

internal val alice = MessageSender(SenderId("alice-id"), "Student")
internal val bob = MessageSender(SenderId("bob-id"), "Student")
internal val carol = MessageSender(SenderId("carol-id"), "Carol")
internal val conversationId = ConversationId("conversation-a")

internal fun conversation(
    id: ConversationId = conversationId,
    senders: List<MessageSender> = listOf(alice, carol)
) = Conversation(id, senders.map { ConversationParticipant(it.id, it.displayName) })

internal fun message(
    sender: MessageSender = alice,
    inConversation: ConversationId = conversationId,
    id: MessageId = MessageId("message-a")
) = Message(id, inConversation, sender.id, sender.displayName, "Hello", 42L)

internal fun messagesRepository(
    conversations: List<Conversation> = listOf(conversation()),
    messages: List<Message> = emptyList()
) = InMemoryMessagesRepository(
    initialConversations = conversations.map { MessagesMapper.toDto(it) },
    initialMessages = messages.groupBy { it.conversationId.value }.mapValues { (_, entries) ->
        entries.map { MessagesMapper.toDto(it) }
    }
)

internal class MutableMessageSenderProvider(
    var result: Result<MessageSender> = Result.Success(alice)
) : CurrentMessageSenderProvider {
    override suspend fun getCurrentSender(): Result<MessageSender> = result
}
