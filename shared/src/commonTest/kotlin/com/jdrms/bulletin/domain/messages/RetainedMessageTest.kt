package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationReadOnlyException
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageEditing
import com.jdrms.bulletin.domain.messages.domain.service.InboxReconciliation
import com.jdrms.bulletin.domain.messages.domain.service.MessageTimeline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class RetainedMessageTest {
    private val retained = message(carol).copy(senderId = null)

    @Test
    fun historyAcceptsDeletedSendersButRejectsUnknownLiveSenders() {
        val thread = conversation(senders = listOf(alice))
        assertEquals(retained, thread.recordMessage(retained).getOrThrow().lastMessage)
        assertEquals(retained, thread.reconcileMessages(listOf(retained)).getOrThrow().lastMessage)
        assertIs<Result.Error>(thread.recordMessage(message(carol)))
        assertIs<Result.Error>(thread.reconcileMessages(listOf(message(carol))))
    }

    @Test
    fun oneRemainingParticipantIsReadOnlyEvenWhenLastMessageIsTheirOwn() {
        val thread = conversation(senders = listOf(alice)).recordMessage(message()).getOrThrow()
        assertTrue(thread.isReadOnly)
        assertIs<ConversationReadOnlyException>(assertIs<Result.Error>(thread.authorizeSending(alice.id)).exception)
        assertIs<Result.Error>(thread.authorizeSending(carol.id))
        assertIs<Result.Error>(thread.authorizeSending(null))
        assertIs<Result.Success<Unit>>(conversation().authorizeSending(alice.id))
    }

    @Test
    fun missingSenderIsNeverEditableEvenWithoutAViewerIdentity() {
        assertEquals("User Not Found", retained.displaySenderName)
        assertFalse(MessageEditing.canEdit(retained, null, retained.orderingTimestamp))
        assertFalse(MessageEditing.canEdit(retained, alice.id, retained.orderingTimestamp))
    }

    @Test
    fun delayedVersionsCannotRestoreDeletedIdentityOrOverwriteNewerEdits() {
        val edited = message(carol).copy(content = "Edited", editedAt = retained.orderingTimestamp + 1.minutes)
        listOf(
            MessageTimeline.merge(listOf(edited), listOf(retained)),
            MessageTimeline.merge(listOf(retained), listOf(edited))
        ).forEach { versions ->
            val result = versions.single()
            assertNull(result.senderId)
            assertEquals(Message.MISSING_SENDER_NAME, result.senderName)
            assertEquals("Edited", result.content)
        }
    }

    @Test
    fun summariesAlsoKeepSenderDetachedAfterDelayedResponses() {
        val deleted = conversation().recordMessage(retained).getOrThrow()
        val stale = conversation().recordMessage(message(carol)).getOrThrow()
        val updated = deleted.recordMessage(message(carol)).getOrThrow()
        assertNull(updated.lastMessage?.senderId)
        val merged = InboxReconciliation.merge(listOf(deleted), listOf(stale)).single()
        assertNull(merged.lastMessage?.senderId)
        assertEquals(Message.MISSING_SENDER_NAME, merged.lastMessage?.senderName)
        val oneMember = Conversation(conversationId, listOf(deleted.participants.first()), retained)
        assertTrue(oneMember.isReadOnly)
    }
}
