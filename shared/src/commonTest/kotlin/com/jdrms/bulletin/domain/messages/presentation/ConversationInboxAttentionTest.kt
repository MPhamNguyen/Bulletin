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
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class ConversationInboxAttentionTest {
    @Test
    fun pendingReviewHasUnreadIndicatorEvenWhenAllMessagesWereRead() {
        val attention = conversationInboxAttention(conversation(), state(pending = true))
        assertTrue(attention.hasUnreadMessage)
        assertTrue(attention.hasPendingReview)
        assertEquals("Leave a review.", attention.previewText)
    }

    @Test
    fun submittedReviewRestoresTheMessagePreviewAndReadState() {
        val attention = conversationInboxAttention(
            conversation(),
            state(pending = true),
            mapOf("listing:1" to ConversationReviewState.Submitted)
        )
        assertFalse(attention.hasPendingReview)
        assertFalse(attention.hasUnreadMessage)
        assertEquals("Thanks!", attention.previewText)
    }

    @Test
    fun failedSubmissionKeepsReminderAndOrdinaryUnreadMessagesRemainUnread() {
        val failed = conversationInboxAttention(
            conversation(),
            state(pending = true),
            mapOf("listing:1" to ConversationReviewState.Failed("Offline"))
        )
        assertEquals("Leave a review.", failed.previewText)
        assertTrue(failed.hasUnreadMessage)
        val unread = conversationInboxAttention(conversation(read = false), state(pending = false))
        assertTrue(unread.hasUnreadMessage)
        assertFalse(unread.hasPendingReview)
        assertEquals("Thanks!", unread.previewText)
    }

    @Test
    fun anotherViewersSnapshotHasNoReviewReminder() {
        val attention = conversationInboxAttention(conversation(), MessagesUiState(viewerId = seller))
        assertFalse(attention.hasPendingReview)
        assertFalse(attention.hasUnreadMessage)
    }

    private fun state(pending: Boolean) = MessagesUiState(
        viewerId = buyer,
        pendingReviewConversationIds = if (pending) setOf(id) else emptySet()
    )

    private fun conversation(read: Boolean = true) = Conversation(
        id,
        listOf(
            ConversationParticipant(buyer, "Buyer", if (read) Instant.fromEpochMilliseconds(100) else null),
            ConversationParticipant(seller, "Seller")
        ),
        lastMessage = Message(MessageId("message"), id, seller, "Seller", "Thanks!", 50),
        listingId = ListingReferenceId("listing:1")
    )

    private val buyer = SenderId("buyer")
    private val seller = SenderId("seller")
    private val id = ConversationId("chat")
}
