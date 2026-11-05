package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.conversation
import com.jdrms.bulletin.domain.messages.conversationId
import com.jdrms.bulletin.domain.messages.message
import com.jdrms.bulletin.domain.messages.secondDraftConversationId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MessageSendStateTest {
    @Test
    fun pendingAndFailureProjectionFollowTheSelectedConversation() {
        val state = MessagesUiState(
            selectedConversationId = conversationId,
            sendStates = mapOf(
                conversationId to MessageSendState.Sending,
                secondDraftConversationId to MessageSendState.Failed("Retry B")
            )
        )
        assertTrue(state.isSending)
        assertNull(state.sendErrorMessage)
        val other = state.copy(selectedConversationId = secondDraftConversationId)
        assertFalse(other.isSending)
        assertEquals("Retry B", other.sendErrorMessage)
    }

    @Test
    fun unrelatedHistoryFailureDoesNotEndPendingSends() {
        val state = MessagesUiState(
            selectedConversationId = conversationId,
            sendStates = mapOf(conversationId to MessageSendState.Sending)
        )
        val presented = state.withFailure(Result.Error(IllegalStateException("History unavailable")))
        assertTrue(presented.state.isSending)
        assertEquals(state.sendStates, presented.state.sendStates)
    }

    @Test
    fun recordingSentMessageClearsOnlyItsOwnPendingState() {
        val state = MessagesUiState(
            selectedConversationId = secondDraftConversationId,
            conversations = listOf(conversation(), conversation(secondDraftConversationId)),
            sendStates = mapOf(
                conversationId to MessageSendState.Sending,
                secondDraftConversationId to MessageSendState.Sending
            ),
            messageInput = "Draft B"
        )
        val updated = state.withSentMessage(conversationId, message()).getOrThrow()
        assertEquals(mapOf(secondDraftConversationId to MessageSendState.Sending), updated.sendStates)
        assertTrue(updated.isSending)
        assertEquals("Draft B", updated.messageInput)
    }
}
