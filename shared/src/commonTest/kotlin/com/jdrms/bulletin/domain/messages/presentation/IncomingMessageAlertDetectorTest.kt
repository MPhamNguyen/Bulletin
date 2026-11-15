package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class IncomingMessageAlertDetectorTest {
    private val me = SenderId("me")
    private val other = SenderId("other")
    private val thread = ConversationId("thread")

    private fun conversation(last: Message?, readAt: Long? = null) = Conversation(
        id = thread,
        participants = listOf(
            ConversationParticipant(me, "Me", readAt?.let(Instant::fromEpochMilliseconds)),
            ConversationParticipant(other, "Other")
        ),
        lastMessage = last,
        listingId = ListingReferenceId("listing")
    )

    private fun message(id: String, from: SenderId = other, at: Long) =
        Message(MessageId(id), thread, from, "Other", "hi $id", at)

    private fun inbox(vararg conversations: Conversation, viewer: SenderId? = me, loading: Boolean = false) =
        InboxState(viewer, conversations.toList(), isLoading = loading)

    @Test
    fun `first loaded snapshot is a baseline and never alerts`() {
        val detector = IncomingMessageAlertDetector()
        assertTrue(detector.detect(inbox(loading = true), null).alerts.isEmpty())
        assertTrue(detector.detect(inbox(conversation(message("m1", at = 1_000))), null).alerts.isEmpty())
    }

    @Test
    fun `failed first load does not seed an empty baseline`() {
        val detector = IncomingMessageAlertDetector()
        detector.detect(InboxState(me, emptyList(), isLoading = false, errorMessage = "failed"), null)
        detector.detect(inbox(conversation(message("m1", at = 1_000))), null)
        val next = detector.detect(inbox(conversation(message("m2", at = 2_000))), null)
        assertEquals(listOf(MessageId("m2")), next.alerts.map { it.messageId })
    }

    @Test
    fun `new incoming unread message alerts once`() {
        val detector = IncomingMessageAlertDetector()
        detector.detect(inbox(conversation(message("m1", at = 1_000), readAt = 1_000)), null)
        val snapshot = inbox(conversation(message("m2", at = 2_000), readAt = 1_000))
        val first = detector.detect(snapshot, null)
        val repeat = detector.detect(snapshot, null)
        assertEquals(listOf(MessageId("m2")), first.alerts.map { it.messageId })
        assertEquals(me, first.alerts.single().recipientId)
        assertEquals(other, first.alerts.single().senderId)
        assertEquals(ListingReferenceId("listing"), first.alerts.single().listingId)
        assertTrue(repeat.alerts.isEmpty())
    }

    @Test
    fun `own messages and the open conversation do not alert`() {
        val detector = IncomingMessageAlertDetector()
        detector.detect(inbox(conversation(message("m1", at = 1_000))), null)
        assertTrue(detector.detect(inbox(conversation(message("m2", from = me, at = 2_000))), null).alerts.isEmpty())
        assertTrue(detector.detect(inbox(conversation(message("m3", at = 3_000))), thread).alerts.isEmpty())
        // Suppressed messages are consumed: leaving the thread later must not replay them.
        assertTrue(detector.detect(inbox(conversation(message("m3", at = 3_000))), null).alerts.isEmpty())
    }

    @Test
    fun `reading a conversation clears its alert`() {
        val detector = IncomingMessageAlertDetector()
        detector.detect(inbox(conversation(message("m1", at = 1_000), readAt = 1_000)), null)
        detector.detect(inbox(conversation(message("m2", at = 2_000), readAt = 1_000)), null)
        val read = detector.detect(inbox(conversation(message("m2", at = 2_000), readAt = 2_000)), null)
        assertEquals(listOf(thread), read.cleared)
    }

    @Test
    fun `an older latest message does not alert`() {
        val detector = IncomingMessageAlertDetector()
        detector.detect(inbox(conversation(message("m2", at = 2_000), readAt = 2_000)), null)
        val regressed = detector.detect(inbox(conversation(message("m1", at = 1_000), readAt = 500)), null)
        assertTrue(regressed.alerts.isEmpty())
    }

    @Test
    fun `a different message with the same timestamp alerts`() {
        val detector = IncomingMessageAlertDetector()
        detector.detect(inbox(conversation(message("m1", at = 1_000), readAt = 500)), null)
        detector.detect(inbox(conversation(message("m2", at = 2_000), readAt = 500)), null)
        val tied = detector.detect(inbox(conversation(message("m3", at = 2_000), readAt = 500)), null)
        assertEquals(listOf(MessageId("m3")), tied.alerts.map { it.messageId })
        val repeat = detector.detect(inbox(conversation(message("m3", at = 2_000), readAt = 500)), null)
        assertTrue(repeat.alerts.isEmpty())
    }

    @Test
    fun `first snapshot clears notifications left by a previous process`() {
        val detector = IncomingMessageAlertDetector()
        assertTrue(detector.detect(inbox(viewer = null), null).clearAll)
        assertTrue(detector.detect(inbox(viewer = null), null).clearAll.not())
    }

    @Test
    fun `signing in from signed out clears notifications`() {
        val detector = IncomingMessageAlertDetector()
        detector.detect(inbox(viewer = null), null)
        assertTrue(detector.detect(inbox(loading = true), null).clearAll)
        assertTrue(detector.detect(inbox(conversation(message("m1", at = 1_000))), null).clearAll.not())
    }

    @Test
    fun `account switch rebaselines and clears alerts`() {
        val detector = IncomingMessageAlertDetector()
        detector.detect(inbox(conversation(message("m1", at = 1_000), readAt = 1_000)), null)
        detector.detect(inbox(conversation(message("m2", at = 2_000), readAt = 1_000)), null)
        val signedOut = detector.detect(inbox(viewer = null), null)
        assertTrue(signedOut.clearAll)
        val next = detector.detect(inbox(conversation(message("m2", at = 2_000), readAt = 1_000)), null)
        assertTrue(next.alerts.isEmpty())
    }

    @Test
    fun `notification requests open only for the addressed account`() {
        val requests = NotificationConversationRequests()
        requests.submit(NotificationConversationRequest(thread, me))
        assertEquals(null, requests.take(null))
        assertEquals(thread, requests.take(me))
        requests.submit(NotificationConversationRequest(thread, me))
        assertEquals(null, requests.take(other))
        assertEquals(null, requests.take(me))
    }
}
