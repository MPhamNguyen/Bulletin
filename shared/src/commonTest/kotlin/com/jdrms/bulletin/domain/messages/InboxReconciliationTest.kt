package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.service.InboxReconciliation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InboxReconciliationTest {
    @Test
    fun olderFetchCannotReplaceNewerAcknowledgedSummary() {
        val newer = message(id = MessageId("newer")).copy(timestampMillis = 20L)
        val old = message(id = MessageId("old")).copy(timestampMillis = 10L)
        val current = conversation().recordMessage(newer).getOrThrow()
        val fetched = conversation().recordMessage(old).getOrThrow()
        assertEquals(newer, InboxReconciliation.merge(listOf(current), listOf(fetched)).single().lastMessage)
    }

    @Test
    fun authoritativeFetchUpdatesEqualMessageAndRemovesMissingConversation() {
        val old = message().copy(timestampMillis = 20L)
        val reported = old.report()
        val current = conversation().recordMessage(old).getOrThrow()
        val fetched = conversation().recordMessage(reported).getOrThrow()
        val merged = InboxReconciliation.merge(
            listOf(current, conversation(ConversationId("gone"))),
            listOf(fetched)
        )
        assertEquals(reported, merged.single().lastMessage)
        assertNull(merged.firstOrNull { it.id.value == "gone" })
    }
}
