package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.MessageCursor
import com.jdrms.bulletin.domain.messages.domain.model.MessagePage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MessageHistoryRetryTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun initialFailureRetriesTheThreadOnceWithoutRefreshingInboxOrClearingDraft() = runTest {
        val fixture = MessageHistoryRetryFixture(dispatcher, listOf(message()))
        fixture.readPage = { historyFailure() }
        advanceUntilIdle()
        val viewModel = fixture.viewModel
        val request = assertNotNull(viewModel.uiState.value.failedHistoryRequest)
        viewModel.onMessageInputChanged("Keep my draft")
        val inboxReads = fixture.inboxReads
        val completion = CompletableDeferred<Result<MessagePage>>()
        fixture.readPage = { completion.await() }
        viewModel.retryHistory(request)
        viewModel.retryHistory(request)
        runCurrent()
        assertTrue(viewModel.uiState.value.isLoadingMessages)
        assertEquals(request, viewModel.uiState.value.failedHistoryRequest)
        assertEquals("Keep my draft", viewModel.uiState.value.messageInput)
        assertEquals(2, fixture.calls.size)
        completion.complete(fixture.storedPage(fixture.calls.last()))
        advanceUntilIdle()
        assertEquals(listOf(message()), viewModel.uiState.value.currentMessages)
        assertNull(viewModel.uiState.value.failedHistoryRequest)
        assertFalse(viewModel.uiState.value.isLoadingMessages)
        assertEquals("Keep my draft", viewModel.uiState.value.messageInput)
        assertEquals(inboxReads, fixture.inboxReads)
        assertEquals(fixture.calls.first(), fixture.calls.last())
    }

    @Test
    fun failedRefreshRetriesItsOriginalHistoryBoundaryAndPreservesLoadedMessages() = runTest {
        val messages = historyMessages()
        val fixture = MessageHistoryRetryFixture(dispatcher, messages)
        advanceUntilIdle()
        val viewModel = fixture.viewModel
        viewModel.loadOlderMessages()
        advanceUntilIdle()
        viewModel.onMessageInputChanged("Draft during reload")
        fixture.readPage = { if (it.before == null) fixture.storedPage(it) else historyFailure() }
        viewModel.onDestinationActiveChanged(false)
        viewModel.onDestinationActiveChanged(true)
        advanceUntilIdle()
        val request = assertNotNull(viewModel.uiState.value.failedHistoryRequest)
        assertEquals(MessageCursor(messages.first().orderingTimestamp, messages.first().id), request.through)
        assertEquals(messages, viewModel.uiState.value.currentMessages)
        val inboxReads = fixture.inboxReads
        val gate = CompletableDeferred<Unit>()
        fixture.readPage = {
            gate.await()
            fixture.storedPage(it)
        }
        viewModel.retryHistory(request)
        runCurrent()
        assertTrue(viewModel.uiState.value.isLoadingMessages)
        assertEquals(messages, viewModel.uiState.value.currentMessages)
        assertEquals("Draft during reload", viewModel.uiState.value.messageInput)
        gate.complete(Unit)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.failedHistoryRequest)
        assertEquals(messages, viewModel.uiState.value.currentMessages)
        assertEquals("Draft during reload", viewModel.uiState.value.messageInput)
        assertEquals(inboxReads, fixture.inboxReads)
        assertEquals(fixture.calls.takeLast(4).take(2), fixture.calls.takeLast(2))
    }

    @Test
    fun retryFromAnotherThreadIsIgnoredAndDoesNotAffectItsDraft() = runTest {
        val fixture = MessageHistoryRetryFixture(dispatcher)
        fixture.readPage = { historyFailure() }
        advanceUntilIdle()
        val viewModel = fixture.viewModel
        val request = assertNotNull(viewModel.uiState.value.failedHistoryRequest)
        fixture.readPage = fixture::storedPage
        viewModel.selectConversation(secondDraftConversationId)
        advanceUntilIdle()
        viewModel.onMessageInputChanged("Draft B")
        val calls = fixture.calls.size
        viewModel.retryHistory(request)
        advanceUntilIdle()
        assertEquals(calls, fixture.calls.size)
        assertNull(viewModel.uiState.value.failedHistoryRequest)
        assertEquals(secondDraftConversationId, viewModel.uiState.value.selectedConversationId)
        assertEquals("Draft B", viewModel.uiState.value.messageInput)
    }

    @Test
    fun lateFailureFromPreviousAccountCannotAttachToTheSameThreadInNewAccount() = runTest {
        val fixture = MessageHistoryRetryFixture(dispatcher)
        val completion = CompletableDeferred<Result<MessagePage>>()
        fixture.readPage = { withContext(NonCancellable) { completion.await() } }
        runCurrent()
        fixture.readPage = fixture::storedPage
        fixture.provider.result = Result.Success(carol)
        fixture.viewModel.loadConversations()
        runCurrent()
        fixture.viewModel.onMessageInputChanged("Carol draft")
        completion.complete(historyFailure())
        advanceUntilIdle()
        assertEquals(carol.id, fixture.viewModel.uiState.value.viewerId)
        assertNull(fixture.viewModel.uiState.value.failedHistoryRequest)
        assertFalse(fixture.viewModel.uiState.value.isLoadingMessages)
        assertEquals("Carol draft", fixture.viewModel.uiState.value.messageInput)
    }
}
