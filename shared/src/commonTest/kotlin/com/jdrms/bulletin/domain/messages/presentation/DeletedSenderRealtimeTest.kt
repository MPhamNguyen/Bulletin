package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.MutableMessageSenderProvider
import com.jdrms.bulletin.domain.messages.alice
import com.jdrms.bulletin.domain.messages.carol
import com.jdrms.bulletin.domain.messages.conversation
import com.jdrms.bulletin.domain.messages.conversationId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.message
import com.jdrms.bulletin.domain.messages.messagesRepository
import com.jdrms.bulletin.domain.messages.testInboxOwner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class DeletedSenderRealtimeTest {
    @Test
    fun senderDetachmentReplacesCachedIdentityAndPreservesHistory() = runTest {
        val inbox = testInboxOwner(
            messagesRepository(),
            MutableMessageSenderProvider(),
            StandardTestDispatcher(testScheduler)
        )
        runCurrent()
        val cached = message(carol)
        val state = MutableStateFlow(
            MessagesUiState(selectedConversationId = conversationId, currentMessages = listOf(cached))
        )
        var failures = 0
        val callbacks = MessagesStreamCallbacksFactory(state, inbox, {}, { failures++ }, { false })
        callbacks.forConversation(conversationId).onReceived(cached.copy(senderId = null))
        runCurrent()
        assertEquals(0, failures)
        assertEquals(cached.content, state.value.currentMessages.single().content)
        assertNull(state.value.currentMessages.single().senderId)
        assertEquals(Message.MISSING_SENDER_NAME, state.value.currentMessages.single().senderName)
        inbox.close()
    }

    @Test
    fun lateEventFromRemovedParticipantRefreshesHistoryWithoutClearingTheThread() = runTest {
        val retained = message(carol).copy(senderId = null, senderName = Message.MISSING_SENDER_NAME)
        val repository = messagesRepository(listOf(conversation(senders = listOf(alice))), listOf(retained))
        val inbox = testInboxOwner(repository, MutableMessageSenderProvider(), StandardTestDispatcher(testScheduler))
        runCurrent()
        val state = MutableStateFlow(
            MessagesUiState(selectedConversationId = conversationId, currentMessages = listOf(retained))
        )
        var failures = 0
        var reloads = 0
        val callbacks = MessagesStreamCallbacksFactory(state, inbox, { reloads++ }, { failures++ }, { false })
        callbacks.forConversation(conversationId).onReceived(message(carol))
        runCurrent()
        assertEquals(0, failures)
        assertEquals(1, reloads)
        assertEquals(listOf(retained), state.value.currentMessages)
        inbox.close()
    }
}
