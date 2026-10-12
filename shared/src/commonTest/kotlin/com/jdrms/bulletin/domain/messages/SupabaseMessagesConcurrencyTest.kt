package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.MessagesInstrumentation
import com.jdrms.bulletin.domain.messages.application.MessagesTimingEvent
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.infrastructure.repository.SupabaseMessagesRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SupabaseMessagesConcurrencyTest {
    @Test
    fun inboxUsesOneBatchReadAndPreservesConversationOrdering() = runTest {
        val conversations = (1..7).map { index ->
            val source = conversation(ConversationId("conversation-$index"))
            Conversation(source.id, source.participants, updatedAtMillis = index.toLong())
        }
        val table = FakeSupabaseMessagesTable(conversations)
        val events = mutableListOf<MessagesTimingEvent>()
        val repository = SupabaseMessagesRepository(table, MessagesInstrumentation(events::add))

        val result = repository.getConversations(alice.id)

        assertEquals(1, table.inboxReadCount)
        assertEquals(
            conversations.sortedByDescending(Conversation::updatedAtMillis).map(Conversation::id),
            assertIs<Result.Success<List<Conversation>>>(result).data.map(Conversation::id)
        )
        assertEquals(1, events.single { it.operation == "inbox_retrieval" }.callCount)
        assertEquals(7, events.single { it.operation == "inbox_retrieval" }.resultCount)
        assertEquals(7, events.single { it.operation == "inbox_mapping" }.resultCount)
    }

    @Test
    fun emptyInboxStillUsesOneBatchRead() = runTest {
        val table = FakeSupabaseMessagesTable(emptyList())

        assertEquals(emptyList(), SupabaseMessagesRepository(table).getConversations(alice.id).getOrThrow())
        assertEquals(1, table.inboxReadCount)
    }
}
