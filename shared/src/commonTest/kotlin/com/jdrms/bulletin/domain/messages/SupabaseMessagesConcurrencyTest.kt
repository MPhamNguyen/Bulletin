package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.infrastructure.repository.SupabaseMessagesRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SupabaseMessagesConcurrencyTest {
    @Test
    fun inboxPreservesConversationOrdering() = runTest {
        val conversations = (1..7).map { index ->
            val source = conversation(ConversationId("conversation-$index"))
            Conversation(source.id, source.participants, updatedAtMillis = index.toLong())
        }
        val table = FakeSupabaseMessagesTable(conversations)
        val repository = SupabaseMessagesRepository(table)

        val result = repository.getConversations(alice.id)

        assertEquals(
            conversations.sortedByDescending(Conversation::updatedAtMillis).map(Conversation::id),
            assertIs<Result.Success<List<Conversation>>>(result).data.map(Conversation::id)
        )
    }
}
