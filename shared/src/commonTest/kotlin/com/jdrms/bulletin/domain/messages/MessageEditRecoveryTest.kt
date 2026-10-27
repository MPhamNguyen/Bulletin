package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.GetConversationMessages
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.MessageCursor
import com.jdrms.bulletin.domain.messages.domain.model.MessageEdit
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.MessagePage
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import com.jdrms.bulletin.domain.messages.domain.service.InboxReconciliation
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class MessageEditRecoveryTest {
    @Test
    fun reconnectRefreshesPreviouslyLoadedMessagesBeyondTheLatestPage() = runTest {
        val history = (0 until 120).map { index ->
            message(id = MessageId("message-$index")).copy(timestampMillis = index.toLong())
        }
        val backing = messagesRepository(messages = history, now = { Instant.fromEpochMilliseconds(1000L) })
        var fetches = 0
        val repository = object : MessagesRepository by backing {
            override suspend fun getMessagePage(
                userId: SenderId,
                conversationId: ConversationId,
                before: MessageCursor?,
                limit: Int
            ): Result<MessagePage> {
                fetches++
                return backing.getMessagePage(userId, conversationId, before, limit)
            }
        }
        val target = history[30]
        backing.editMessage(alice.id, MessageEdit(conversationId, target.id, "Missed edit", null)).getOrThrow()
        val oldestLoaded = history[30]
        val page = GetConversationMessages(repository, MutableMessageSenderProvider())(
            conversationId,
            through = MessageCursor(oldestLoaded.orderingTimestamp, oldestLoaded.id)
        ).getOrThrow()
        assertEquals(3, fetches)
        assertEquals("Missed edit", page.messages.first { it.id == target.id }.content)
        assertEquals(history.drop(30).map { it.id }, page.messages.map { it.id })
    }

    @Test
    fun staleInboxResponseCannotUndoALocallyAcknowledgedEdit() {
        val original = message()
        val saved = original.copy(content = "Edited preview", editedAt = Instant.fromEpochMilliseconds(1000L))
        val current = conversation().recordMessage(saved).getOrThrow()
        val stale = conversation().recordMessage(original).getOrThrow()
        val merged = InboxReconciliation.merge(listOf(current), listOf(stale)).single()
        assertEquals(saved, merged.lastMessage)
        assertEquals(current.updatedAtMillis, merged.updatedAtMillis)
    }
}
