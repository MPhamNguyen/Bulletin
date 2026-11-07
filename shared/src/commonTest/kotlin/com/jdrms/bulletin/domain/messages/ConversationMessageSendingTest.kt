package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.presentation.MessageSendState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationMessageSendingTest {
    private val dispatcher = StandardTestDispatcher()
    private val provider = MutableMessageSenderProvider()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun pendingSendStaysWithItsThreadAndReturningCannotSubmitItTwice() = runTest {
        val pending = PendingMessageSends()
        val viewModel = draftViewModel(pending.repository, provider, dispatcher)
        advanceUntilIdle()
        viewModel.selectConversation(conversationId)
        advanceUntilIdle()
        viewModel.onMessageInputChanged("Send A")
        viewModel.sendCurrentMessage()
        runCurrent()
        assertTrue(viewModel.uiState.value.isSending)
        viewModel.selectConversation(secondDraftConversationId)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isSending)
        assertNull(viewModel.uiState.value.errorMessage)
        viewModel.onMessageInputChanged("Draft B")
        assertEquals("Draft B", viewModel.uiState.value.messageInput)
        viewModel.selectConversation(conversationId)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isSending)
        viewModel.sendCurrentMessage()
        runCurrent()
        assertEquals(1, pending.requests.size)
        pending.succeed(0)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isSending)
    }

    @Test
    fun completingAWhileBSendsDoesNotUnlockBOrChangeItsDraftAndHistory() = runTest {
        val pending = PendingMessageSends()
        val viewModel = draftViewModel(pending.repository, provider, dispatcher)
        advanceUntilIdle()
        viewModel.selectConversation(conversationId)
        advanceUntilIdle()
        viewModel.onMessageInputChanged("Send A")
        viewModel.sendCurrentMessage()
        runCurrent()
        viewModel.selectConversation(secondDraftConversationId)
        advanceUntilIdle()
        viewModel.onMessageInputChanged("Send B")
        viewModel.sendCurrentMessage()
        runCurrent()
        assertEquals(listOf(conversationId, secondDraftConversationId), pending.requests.map { it.conversationId })
        pending.succeed(0)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isSending)
        assertEquals("Send B", viewModel.uiState.value.messageInput)
        assertEquals(emptyList(), viewModel.uiState.value.currentMessages)
        assertEquals(setOf(secondDraftConversationId), viewModel.uiState.value.sendStates.keys)
        pending.succeed(1)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isSending)
        assertEquals("", viewModel.uiState.value.messageInput)
        assertEquals(listOf("Send B"), viewModel.uiState.value.currentMessages.map { it.content })
    }

    @Test
    fun completingBBeforeAAlsoKeepsAPendingAndClearsOnlyBDraft() = runTest {
        val pending = PendingMessageSends()
        val viewModel = draftViewModel(pending.repository, provider, dispatcher)
        advanceUntilIdle()
        viewModel.selectConversation(conversationId)
        advanceUntilIdle()
        viewModel.onMessageInputChanged("Send A")
        viewModel.sendCurrentMessage()
        runCurrent()
        viewModel.selectConversation(secondDraftConversationId)
        advanceUntilIdle()
        viewModel.onMessageInputChanged("Send B")
        viewModel.sendCurrentMessage()
        runCurrent()
        viewModel.selectConversation(conversationId)
        advanceUntilIdle()
        pending.succeed(1)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isSending)
        assertEquals("Send A", viewModel.uiState.value.messageInput)
        pending.succeed(0)
        advanceUntilIdle()
        viewModel.selectConversation(secondDraftConversationId)
        assertFalse(viewModel.uiState.value.isSending)
        assertEquals("", viewModel.uiState.value.messageInput)
    }

    @Test
    fun backgroundFailureReturnsWithItsDraftAndRetryClearsOnlyThatThreadsError() = runTest {
        val pending = PendingMessageSends()
        val viewModel = draftViewModel(pending.repository, provider, dispatcher)
        advanceUntilIdle()
        viewModel.selectConversation(conversationId)
        advanceUntilIdle()
        viewModel.onMessageInputChanged("Retry A")
        viewModel.sendCurrentMessage()
        runCurrent()
        viewModel.selectConversation(secondDraftConversationId)
        advanceUntilIdle()
        viewModel.onMessageInputChanged("Draft B")
        pending.fail(0)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSending)
        assertEquals("Draft B", viewModel.uiState.value.messageInput)
        viewModel.selectConversation(conversationId)
        advanceUntilIdle()
        assertEquals("Retry A", viewModel.uiState.value.messageInput)
        assertEquals(
            "Unable to send message. Check your connection and try again.",
            viewModel.uiState.value.errorMessage
        )
        viewModel.selectConversation(secondDraftConversationId)
        assertNull(viewModel.uiState.value.errorMessage)
        viewModel.selectConversation(conversationId)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)
        viewModel.sendCurrentMessage()
        assertTrue(viewModel.uiState.value.isSending)
        assertNull(viewModel.uiState.value.errorMessage)
        runCurrent()
        pending.succeed(1)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals("", viewModel.uiState.value.messageInput)
    }

    @Test
    fun failingADoesNotEndBPendingSendAndBothFailuresStaySeparate() = runTest {
        val pending = PendingMessageSends()
        val viewModel = draftViewModel(pending.repository, provider, dispatcher)
        advanceUntilIdle()
        viewModel.selectConversation(conversationId)
        advanceUntilIdle()
        viewModel.onMessageInputChanged("Retry A")
        viewModel.sendCurrentMessage()
        runCurrent()
        viewModel.selectConversation(secondDraftConversationId)
        advanceUntilIdle()
        viewModel.onMessageInputChanged("Retry B")
        viewModel.sendCurrentMessage()
        runCurrent()
        pending.fail(0)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isSending)
        assertNull(viewModel.uiState.value.errorMessage)
        pending.fail(1)
        advanceUntilIdle()
        assertIs<MessageSendState.Failed>(viewModel.uiState.value.sendStates[conversationId])
        assertIs<MessageSendState.Failed>(viewModel.uiState.value.sendStates[secondDraftConversationId])
        viewModel.selectConversation(conversationId)
        viewModel.onMessageInputChanged("Revised A")
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(setOf(secondDraftConversationId), viewModel.uiState.value.sendStates.keys)
        viewModel.selectConversation(secondDraftConversationId)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertEquals("Retry B", viewModel.uiState.value.messageInput)
    }

    @Test
    fun oldAccountsConcurrentResultsCannotChangeNewAccountsPendingSend() = runTest {
        val pending = PendingMessageSends()
        val viewModel = draftViewModel(pending.repository, provider, dispatcher)
        advanceUntilIdle()
        viewModel.selectConversation(conversationId)
        advanceUntilIdle()
        viewModel.onMessageInputChanged("Alice A")
        viewModel.sendCurrentMessage()
        runCurrent()
        viewModel.selectConversation(secondDraftConversationId)
        advanceUntilIdle()
        viewModel.onMessageInputChanged("Alice B")
        viewModel.sendCurrentMessage()
        runCurrent()
        provider.result = Result.Success(carol)
        viewModel.loadConversations()
        advanceUntilIdle()
        assertEquals(emptyMap(), viewModel.uiState.value.sendStates)
        viewModel.selectConversation(conversationId)
        advanceUntilIdle()
        viewModel.onMessageInputChanged("Carol A")
        viewModel.sendCurrentMessage()
        runCurrent()
        pending.fail(0)
        pending.succeed(1)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isSending)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals("Carol A", viewModel.uiState.value.messageInput)
        pending.succeed(2)
        advanceUntilIdle()
        assertEquals(emptyMap(), viewModel.uiState.value.sendStates)
        assertEquals(carol.id, viewModel.uiState.value.currentMessages.single().senderId)
    }
}
