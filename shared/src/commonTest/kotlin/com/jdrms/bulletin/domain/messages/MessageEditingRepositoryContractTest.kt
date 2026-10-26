package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageEdit
import com.jdrms.bulletin.domain.messages.domain.model.MessageEditException
import com.jdrms.bulletin.domain.messages.domain.model.MessageEditFailure
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import com.jdrms.bulletin.domain.messages.infrastructure.repository.SupabaseMessagesRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.minutes

abstract class MessageEditingRepositoryContractTest {
    protected val stored = message()
    protected var clock = stored.orderingTimestamp + 1.minutes
    protected abstract fun repository(messages: List<Message>): MessagesRepository
    private val edit get() = MessageEdit(conversationId, stored.id, "Changed", null)

    @Test
    fun editsArePersistedWithoutChangingMessageOrderOrConversationActivity() = runTest {
        val repository = repository(listOf(stored))
        val saved = repository.editMessage(alice.id, edit).getOrThrow()
        assertEquals(stored.copy(content = "Changed", editedAt = clock), saved)
        assertEquals(saved, repository.getMessagePage(alice.id, conversationId).getOrThrow().messages.single())
        val summary = repository.getConversations(alice.id).getOrThrow().single()
        assertEquals(saved, summary.lastMessage)
        assertEquals(stored.timestampMillis, summary.updatedAtMillis)
        failure(MessageEditFailure.CONFLICT, repository.editMessage(alice.id, edit.copy(content = "Stale")))
    }

    @Test
    fun inaccessibleDeletedAndReportedMessagesCannotBeEdited() = runTest {
        failure(
            MessageEditFailure.UNAVAILABLE,
            repository(listOf(stored.copy(senderId = carol.id))).editMessage(alice.id, edit)
        )
        failure(
            MessageEditFailure.UNAVAILABLE,
            repository(listOf(stored.copy(deletedAt = clock))).editMessage(alice.id, edit)
        )
        val reported = repository(listOf(stored))
        reported.reportMessage(alice.id, conversationId, stored.id, "Spam").getOrThrow()
        failure(MessageEditFailure.REPORTED, reported.editMessage(alice.id, edit))
        failure(
            MessageEditFailure.UNAVAILABLE,
            reported.editMessage(alice.id, edit.copy(messageId = MessageId("missing")))
        )
    }

    @Test
    fun hiddenHistoryAndExpiredMessagesCannotBeEdited() = runTest {
        val hidden = repository(listOf(stored))
        hidden.hideConversation(alice.id, conversationId).getOrThrow()
        failure(MessageEditFailure.UNAVAILABLE, hidden.editMessage(alice.id, edit))
        clock = stored.orderingTimestamp + 15.minutes
        failure(MessageEditFailure.EXPIRED, repository(listOf(stored)).editMessage(alice.id, edit))
    }

    private fun failure(expected: MessageEditFailure, result: Result<Message>) {
        assertEquals(expected, assertIs<MessageEditException>(assertIs<Result.Error>(result).exception).failure)
    }
}

class InMemoryMessageEditingRepositoryTest : MessageEditingRepositoryContractTest() {
    override fun repository(messages: List<Message>): MessagesRepository = messagesRepository(
        conversations = listOf(conversation().recordMessage(stored).getOrThrow()),
        messages = messages,
        now = { clock }
    )
}

class SupabaseMessageEditingRepositoryTest : MessageEditingRepositoryContractTest() {
    override fun repository(messages: List<Message>): MessagesRepository = SupabaseMessagesRepository(
        FakeSupabaseMessagesTable(
            listOf(conversation().recordMessage(stored).getOrThrow()),
            messages,
            now = { clock }
        )
    )
}
