package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConversationTest {
    private val sender = SenderId("user-a")
    private val participant = ConversationParticipant(sender, "Same Name")
    private val conversationId = ConversationId("conversation")

    @Test
    fun senderIdentityMustNotBeBlank() {
        listOf("", "   ").forEach { value ->
            assertFailsWith<IllegalArgumentException> { SenderId(value) }
        }
    }

    @Test
    fun conversationRequiresUniqueParticipants() {
        assertFailsWith<IllegalArgumentException> { Conversation(conversationId, emptyList()) }
        assertFailsWith<IllegalArgumentException> { Conversation(conversationId, listOf(participant, participant)) }
    }

    @Test
    fun membershipUsesIdentityAndDefensivelyCopiesParticipants() {
        val participants = mutableListOf(participant)
        val conversation = Conversation(conversationId, participants)
        participants.clear()
        assertTrue(conversation.includes(sender))
        assertFalse(conversation.includes(SenderId("user-b")))
        assertEquals(listOf("Same Name"), conversation.participantNames)
    }

    @Test
    fun participantMessageUpdatesSummaryWithoutMutatingOriginal() {
        val conversation = Conversation(conversationId, listOf(participant))
        val message = message()
        val updated = conversation.recordMessage(message).getOrThrow()
        assertEquals(message, updated.lastMessage)
        assertEquals(42L, updated.updatedAtMillis)
        assertEquals(null, conversation.lastMessage)
    }

    @Test
    fun nonParticipantAndMismatchedConversationCannotRecordMessages() {
        val conversation = Conversation(conversationId, listOf(participant))
        assertTrue(conversation.recordMessage(message().copy(senderId = SenderId("other"))).isError())
        assertTrue(conversation.recordMessage(message().copy(conversationId = ConversationId("other"))).isError())
        assertFailsWith<IllegalArgumentException> {
            Conversation(conversationId, listOf(participant), message().copy(senderId = SenderId("other")))
        }
        assertFailsWith<IllegalArgumentException> {
            Conversation(ConversationId("other"), listOf(participant), message())
        }
    }

    @Test
    fun reportingPreservesMessageIdentityAndContent() {
        val message = message()
        assertEquals(message.copy(isReported = true), message.report())
        assertFalse(message.isReported)
    }

    private fun message() = Message(MessageId("message"), conversationId, sender, "Same Name", "Hello", 42L)
}
