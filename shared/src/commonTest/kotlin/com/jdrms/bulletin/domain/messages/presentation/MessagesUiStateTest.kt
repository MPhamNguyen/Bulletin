package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class MessagesUiStateTest {
    @Test
    fun missingConversationFinishesItsMessageLoadingState() {
        val conversationId = ConversationId("missing")
        val state = MessagesUiState(
            selectedConversationId = conversationId,
            isLoadingMessages = true
        )

        val updated = state.clearMessageLoadingForMissingConversation(conversationId)

        assertFalse(updated.isLoadingMessages)
    }

    @Test
    fun missingConversationDoesNotClearLoadingForAnotherSelection() {
        val loadingConversationId = ConversationId("loading")
        val selectedConversationId = ConversationId("selected")
        val state = MessagesUiState(
            selectedConversationId = selectedConversationId,
            isLoadingMessages = true
        )

        val updated = state.clearMessageLoadingForMissingConversation(loadingConversationId)

        assertEquals(state, updated)
    }
}
