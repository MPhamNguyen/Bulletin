package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
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
class OlderMessageHistoryRetryTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun retryRepeatsTheFailedOlderPageAndRetainsMessagesAndDraftDuringLoading() = runTest {
        val messages = historyMessages()
        val fixture = MessageHistoryRetryFixture(dispatcher, messages)
        advanceUntilIdle()
        val viewModel = fixture.viewModel
        val loaded = viewModel.uiState.value.currentMessages
        val cursor = assertNotNull(viewModel.uiState.value.olderCursor)
        viewModel.onMessageInputChanged("Draft while paging")
        fixture.readPage = { historyFailure() }
        viewModel.loadOlderMessages()
        advanceUntilIdle()
        val request = assertNotNull(viewModel.uiState.value.failedOlderHistoryRequest)
        assertEquals(cursor, request.before)
        assertEquals(loaded, viewModel.uiState.value.currentMessages)
        val reads = fixture.inboxReads
        val completion = CompletableDeferred<Result<MessagePage>>()
        fixture.readPage = { completion.await() }
        viewModel.retryHistory(request)
        viewModel.retryHistory(request)
        runCurrent()
        assertEquals(3, fixture.calls.size)
        assertTrue(viewModel.uiState.value.isLoadingOlder)
        assertEquals(cursor, viewModel.uiState.value.olderCursor)
        assertEquals(loaded, viewModel.uiState.value.currentMessages)
        assertEquals("Draft while paging", viewModel.uiState.value.messageInput)
        completion.complete(fixture.storedPage(fixture.calls.last()))
        advanceUntilIdle()
        assertEquals(messages, viewModel.uiState.value.currentMessages)
        assertNull(viewModel.uiState.value.failedOlderHistoryRequest)
        assertNull(viewModel.uiState.value.olderCursor)
        assertFalse(viewModel.uiState.value.isLoadingOlder)
        assertEquals("Draft while paging", viewModel.uiState.value.messageInput)
        assertEquals(reads, fixture.inboxReads)
        assertEquals(fixture.calls[1], fixture.calls[2])
    }

    @Test
    fun automaticPaginationWaitsForExplicitRetryAfterFailureAndResumesAfterRecovery() = runTest {
        val fixture = MessageHistoryRetryFixture(dispatcher, historyMessages(65))
        advanceUntilIdle()
        val viewModel = fixture.viewModel
        fixture.readPage = { historyFailure() }
        viewModel.loadOlderMessages()
        advanceUntilIdle()
        val request = assertNotNull(viewModel.uiState.value.failedOlderHistoryRequest)
        val callsAfterFailure = fixture.calls.size
        repeat(3) {
            viewModel.loadOlderMessages()
            advanceUntilIdle()
        }
        assertEquals(callsAfterFailure, fixture.calls.size)
        assertEquals(request, viewModel.uiState.value.failedOlderHistoryRequest)
        fixture.readPage = fixture::storedPage
        viewModel.retryHistory(request)
        advanceUntilIdle()
        assertEquals(callsAfterFailure + 1, fixture.calls.size)
        assertEquals(fixture.calls[1], fixture.calls[2])
        assertNull(viewModel.uiState.value.failedOlderHistoryRequest)
        assertNotNull(viewModel.uiState.value.olderCursor)
        viewModel.loadOlderMessages()
        advanceUntilIdle()
        assertEquals(callsAfterFailure + 2, fixture.calls.size)
        assertEquals(historyMessages(65), viewModel.uiState.value.currentMessages)
        assertNull(viewModel.uiState.value.olderCursor)
    }

    @Test
    fun anotherFailureRetainsTheSameOlderCursorForTheNextRetry() = runTest {
        val fixture = MessageHistoryRetryFixture(dispatcher, historyMessages())
        advanceUntilIdle()
        val viewModel = fixture.viewModel
        fixture.readPage = { historyFailure() }
        viewModel.loadOlderMessages()
        advanceUntilIdle()
        val request = assertNotNull(viewModel.uiState.value.failedOlderHistoryRequest)
        viewModel.retryHistory(request)
        advanceUntilIdle()
        assertEquals(request, viewModel.uiState.value.failedOlderHistoryRequest)
        assertEquals(request.before, viewModel.uiState.value.olderCursor)
        assertFalse(viewModel.uiState.value.isLoadingOlder)
        fixture.readPage = fixture::storedPage
        viewModel.retryHistory(request)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.failedOlderHistoryRequest)
        assertEquals(historyMessages(), viewModel.uiState.value.currentMessages)
    }

    @Test
    fun successfulThreadRefreshDoesNotClearAnOlderPageRetryOrItsLoadingFlag() = runTest {
        val fixture = MessageHistoryRetryFixture(dispatcher, historyMessages())
        advanceUntilIdle()
        val viewModel = fixture.viewModel
        fixture.readPage = { historyFailure() }
        viewModel.loadOlderMessages()
        advanceUntilIdle()
        val request = assertNotNull(viewModel.uiState.value.failedOlderHistoryRequest)
        val completion = CompletableDeferred<Result<MessagePage>>()
        fixture.readPage = { if (it.before == null) fixture.storedPage(it) else completion.await() }
        viewModel.retryHistory(request)
        runCurrent()
        viewModel.onDestinationActiveChanged(false)
        viewModel.onDestinationActiveChanged(true)
        runCurrent()
        assertFalse(viewModel.uiState.value.isLoadingMessages)
        assertTrue(viewModel.uiState.value.isLoadingOlder)
        assertEquals(request, viewModel.uiState.value.failedOlderHistoryRequest)
        completion.complete(fixture.storedPage(fixture.calls.first { it.before == request.before }))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoadingOlder)
        assertNull(viewModel.uiState.value.failedOlderHistoryRequest)
    }

    @Test
    fun cancelledOlderPageFailureCannotReplaceTheNewThreadsStateOrBecomeRetryable() = runTest {
        val fixture = MessageHistoryRetryFixture(dispatcher, historyMessages())
        advanceUntilIdle()
        val viewModel = fixture.viewModel
        val completion = CompletableDeferred<Result<MessagePage>>()
        fixture.readPage = { withContext(NonCancellable) { completion.await() } }
        viewModel.loadOlderMessages()
        runCurrent()
        fixture.readPage = fixture::storedPage
        viewModel.selectConversation(secondDraftConversationId)
        runCurrent()
        viewModel.onMessageInputChanged("Draft B")
        completion.complete(historyFailure())
        advanceUntilIdle()
        assertEquals(secondDraftConversationId, viewModel.uiState.value.selectedConversationId)
        assertNull(viewModel.uiState.value.failedOlderHistoryRequest)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals("Draft B", viewModel.uiState.value.messageInput)
        assertFalse(viewModel.uiState.value.isLoadingOlder)
    }
}
