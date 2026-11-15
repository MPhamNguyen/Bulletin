package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.MessagingAuthenticationRequiredException
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import kotlinx.coroutines.CompletableDeferred
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
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class MessageSendingDraftsTest {
    private val dispatcher = StandardTestDispatcher()
    private val provider = MutableMessageSenderProvider()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun successfulSendClearsOnlyTheSentConversationsDraft() = runTest {
        val repository = draftRepository()
        val viewModel = draftViewModel(repository, provider, dispatcher)
        advanceUntilIdle()
        viewModel.selectConversation(secondDraftConversationId)
        viewModel.onMessageInputChanged("Draft B")
        viewModel.selectConversation(conversationId)
        advanceUntilIdle()
        viewModel.onMessageInputChanged(" Send A ")
        viewModel.sendCurrentMessage()
        advanceUntilIdle()
        assertEquals("", viewModel.uiState.value.messageInput)
        assertEquals("Send A", repository.getMessages(alice.id, conversationId).getOrThrow().single().content)
        viewModel.selectConversation(secondDraftConversationId)
        assertEquals("Draft B", viewModel.uiState.value.messageInput)
        viewModel.selectConversation(conversationId)
        assertEquals("", viewModel.uiState.value.messageInput)
    }

    @Test
    fun failedSendKeepsDraftAvailableAfterSwitching() = runTest {
        val backing = draftRepository()
        val repository = object : MessagesRepository by backing {
            override suspend fun sendMessage(message: Message): Result<Message> =
                Result.Error(IllegalStateException("Offline"))
        }
        val viewModel = draftViewModel(repository, provider, dispatcher)
        advanceUntilIdle()
        viewModel.selectConversation(conversationId)
        advanceUntilIdle()
        viewModel.onMessageInputChanged("Retry this")
        viewModel.sendCurrentMessage()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSending)
        viewModel.selectConversation(secondDraftConversationId)
        viewModel.selectConversation(conversationId)
        assertEquals("Retry this", viewModel.uiState.value.messageInput)
    }

    @Test
    fun sendCompletingInBackgroundClearsStoredDraftWithoutChangingCurrentThread() = runTest {
        val completion = CompletableDeferred<Result<Message>>()
        val repository = pendingRepository(completion)
        val viewModel = draftViewModel(repository, provider, dispatcher)
        advanceUntilIdle()
        viewModel.selectConversation(conversationId)
        advanceUntilIdle()
        viewModel.onMessageInputChanged("Send A")
        viewModel.sendCurrentMessage()
        runCurrent()
        viewModel.selectConversation(secondDraftConversationId)
        advanceUntilIdle()
        viewModel.onMessageInputChanged("Draft B")
        completion.complete(Result.Success(message().copy(content = "Send A")))
        advanceUntilIdle()
        assertEquals("Draft B", viewModel.uiState.value.messageInput)
        assertEquals(emptyList(), viewModel.uiState.value.currentMessages)
        assertFalse(viewModel.uiState.value.isSending)
        viewModel.selectConversation(conversationId)
        assertEquals("", viewModel.uiState.value.messageInput)
    }

    @Test
    fun lateSendDoesNotClearNewerTextInTheSameConversation() = runTest {
        val completion = CompletableDeferred<Result<Message>>()
        val viewModel = draftViewModel(pendingRepository(completion), provider, dispatcher)
        advanceUntilIdle()
        viewModel.selectConversation(conversationId)
        advanceUntilIdle()
        viewModel.onMessageInputChanged("First reply")
        viewModel.sendCurrentMessage()
        runCurrent()
        viewModel.onMessageInputChanged("Next reply")
        completion.complete(Result.Success(message().copy(content = "First reply")))
        advanceUntilIdle()
        assertEquals("Next reply", viewModel.uiState.value.messageInput)
        viewModel.selectConversation(secondDraftConversationId)
        viewModel.selectConversation(conversationId)
        assertEquals("Next reply", viewModel.uiState.value.messageInput)
    }

    @Test
    fun backgroundSendFailurePreservesBothConversationsDrafts() = runTest {
        val completion = CompletableDeferred<Result<Message>>()
        val viewModel = draftViewModel(pendingRepository(completion), provider, dispatcher)
        advanceUntilIdle()
        viewModel.selectConversation(conversationId)
        advanceUntilIdle()
        viewModel.onMessageInputChanged("Retry A")
        viewModel.sendCurrentMessage()
        runCurrent()
        viewModel.selectConversation(secondDraftConversationId)
        viewModel.onMessageInputChanged("Draft B")
        completion.complete(Result.Error(IllegalStateException("Offline")))
        advanceUntilIdle()
        assertEquals("Draft B", viewModel.uiState.value.messageInput)
        assertFalse(viewModel.uiState.value.isSending)
        viewModel.selectConversation(conversationId)
        assertEquals("Retry A", viewModel.uiState.value.messageInput)
    }

    @Test
    fun oldSessionSendCannotClearAnotherAccountsDraftOrMutateItsThread() = runTest {
        val completion = CompletableDeferred<Result<Message>>()
        val viewModel = draftViewModel(pendingRepository(completion), provider, dispatcher)
        advanceUntilIdle()
        viewModel.selectConversation(conversationId)
        advanceUntilIdle()
        viewModel.onMessageInputChanged("Same text")
        viewModel.sendCurrentMessage()
        runCurrent()
        provider.result = Result.Success(carol)
        viewModel.loadConversations()
        advanceUntilIdle()
        viewModel.selectConversation(conversationId)
        viewModel.onMessageInputChanged("Same text")
        completion.complete(Result.Success(message().copy(content = "Same text")))
        advanceUntilIdle()
        assertEquals("Same text", viewModel.uiState.value.messageInput)
        assertEquals(emptyList(), viewModel.uiState.value.currentMessages)
        assertFalse(viewModel.uiState.value.isSending)
    }

    @Test
    fun previousLoginSendCannotClearDraftAfterSigningBackIntoTheSameAccount() = runTest {
        val completion = CompletableDeferred<Result<Message>>()
        val viewModel = draftViewModel(pendingRepository(completion), provider, dispatcher)
        advanceUntilIdle()
        viewModel.selectConversation(conversationId)
        advanceUntilIdle()
        viewModel.onMessageInputChanged("Same text")
        viewModel.sendCurrentMessage()
        runCurrent()
        provider.result = Result.Error(MessagingAuthenticationRequiredException())
        advanceUntilIdle()
        provider.result = Result.Success(alice)
        advanceUntilIdle()
        viewModel.selectConversation(conversationId)
        viewModel.onMessageInputChanged("Same text")
        completion.complete(Result.Success(message().copy(content = "Same text")))
        advanceUntilIdle()
        assertEquals("Same text", viewModel.uiState.value.messageInput)
        assertEquals(emptyList(), viewModel.uiState.value.currentMessages)
    }

    private fun pendingRepository(completion: CompletableDeferred<Result<Message>>): MessagesRepository {
        val backing = draftRepository()
        return object : MessagesRepository by backing {
            override suspend fun sendMessage(message: Message): Result<Message> = completion.await()
        }
    }
}
