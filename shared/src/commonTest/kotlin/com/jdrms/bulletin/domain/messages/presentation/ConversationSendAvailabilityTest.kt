package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.alice
import com.jdrms.bulletin.domain.messages.carol
import com.jdrms.bulletin.domain.messages.conversation
import com.jdrms.bulletin.domain.messages.conversationId
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConversationSendAvailabilityTest {
    private val state = MessagesUiState(
        viewerId = alice.id,
        conversations = listOf(conversation()),
        selectedConversationId = conversationId
    )

    @Test
    fun optionalPortPreservesExistingSendBehavior() {
        assertTrue(state.canSendToCounterpart())
    }

    @Test
    fun requiredStatusMustSucceedBeforeSendingAndBlockDisablesSending() {
        val unchecked = state.copy(block = ConversationBlockUiState(statusRequired = true))
        assertFalse(unchecked.canSendToCounterpart())
        val checked = unchecked.copy(block = unchecked.block.copy(checkedUserIds = setOf(carol.id)))
        assertTrue(checked.canSendToCounterpart())
        assertFalse(
            checked.copy(block = checked.block.copy(blockedUserIds = setOf(carol.id)))
                .canSendToCounterpart()
        )
    }
}
