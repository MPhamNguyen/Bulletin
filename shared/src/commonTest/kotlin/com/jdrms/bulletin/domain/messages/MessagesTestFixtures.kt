package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.CurrentMessageSenderProvider
import com.jdrms.bulletin.domain.messages.application.GetConversations
import com.jdrms.bulletin.domain.messages.application.InboxIdentity
import com.jdrms.bulletin.domain.messages.application.MessageSender
import com.jdrms.bulletin.domain.messages.application.ObserveInboxInvalidations
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.repository.InboxInvalidation
import com.jdrms.bulletin.domain.messages.domain.repository.InboxObserver
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import com.jdrms.bulletin.domain.messages.infrastructure.mapper.MessagesMapper
import com.jdrms.bulletin.domain.messages.infrastructure.repository.InMemoryMessagesRepository
import com.jdrms.bulletin.domain.messages.presentation.InboxStateOwner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Clock
import kotlin.time.Instant

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
    messages: List<Message> = emptyList(),
    now: () -> Instant = { Clock.System.now() }
) = InMemoryMessagesRepository(
    initialConversations = conversations.map { MessagesMapper.toDto(it) },
    initialMessages = messages.groupBy { it.conversationId.value }.mapValues { (_, entries) ->
        entries.map { MessagesMapper.toDto(it) }
    },
    now = now
)

internal class MutableMessageSenderProvider(
    initialResult: Result<MessageSender> = Result.Success(alice)
) : CurrentMessageSenderProvider, InboxIdentity {
    private val userIds = MutableStateFlow((initialResult as? Result.Success)?.data?.id)
    var result: Result<MessageSender> = initialResult
        set(value) {
            field = value
            userIds.value = (value as? Result.Success)?.data?.id
        }

    override suspend fun getCurrentSender(): Result<MessageSender> = result
    override val currentUserId: SenderId? get() = (result as? Result.Success)?.data?.id
    override fun observeUserIds(): Flow<SenderId?> = userIds
}

internal fun testInboxOwner(
    repository: MessagesRepository,
    provider: MutableMessageSenderProvider,
    dispatcher: CoroutineDispatcher
): InboxStateOwner {
    val observer = repository as? InboxObserver ?: InboxObserver { flowOf(InboxInvalidation.Subscribed) }
    return InboxStateOwner(
        GetConversations(repository, provider),
        ObserveInboxInvalidations(observer),
        provider,
        CoroutineScope(SupervisorJob() + dispatcher)
    )
}
