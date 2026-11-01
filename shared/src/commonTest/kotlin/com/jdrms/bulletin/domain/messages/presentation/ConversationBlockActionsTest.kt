package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.ConversationUserBlocker
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.*

class ConversationBlockActionsTest {
    private val target = BlockTarget(SenderId("other"), "Other student")

    @Test
    fun requiresConfirmationAndDeduplicatesSubmissions() = runTest {
        val pending = CompletableDeferred<Result<Unit>>()
        val port = BlockerFake().apply { blockCall = { pending.await() } }
        val state = MutableStateFlow(MessagesUiState())
        val actions = ConversationBlockActions(state, port, backgroundScope)
        actions.confirm()
        actions.request(target)
        runCurrent()
        assertEquals(0, port.blocks)
        actions.dismiss()
        assertIs<ConversationBlockFlow.Idle>(state.value.block.flow)
        actions.request(target)
        actions.confirm()
        actions.confirm()
        actions.dismiss()
        runCurrent()
        assertEquals(1, port.blocks)
        assertFalse(state.value.block.isBlocked(target.userId))
        pending.complete(Result.Success(Unit))
        runCurrent()
        assertTrue(state.value.block.isBlocked(target.userId))
        assertIs<ConversationBlockFlow.Idle>(state.value.block.flow)
    }

    @Test
    fun blockFailureKeepsConfirmationAndAllowsRetry() = runTest {
        val port = BlockerFake().apply { blockCall = { Result.Error(Exception(), "Offline") } }
        val state = MutableStateFlow(MessagesUiState())
        val actions = ConversationBlockActions(state, port, backgroundScope)
        actions.request(target)
        actions.confirm()
        runCurrent()
        assertFalse(state.value.block.isBlocked(target.userId))
        assertEquals("Offline", assertIs<ConversationBlockFlow.Confirming>(state.value.block.flow).errorMessage)
        port.blockCall = { Result.Success(Unit) }
        actions.confirm()
        runCurrent()
        assertTrue(state.value.block.isBlocked(target.userId))
    }

    @Test
    fun unblockChangesStateOnlyAfterSuccess() = runTest {
        val port = BlockerFake().apply { unblockResult = Result.Error(Exception(), "Offline") }
        val state = MutableStateFlow(
            MessagesUiState(
                block =
                ConversationBlockUiState(blockedUserIds = setOf(target.userId))
            )
        )
        val actions = ConversationBlockActions(state, port, backgroundScope)
        actions.unblock(target)
        runCurrent()
        assertTrue(state.value.block.isBlocked(target.userId))
        assertEquals("Offline", state.value.block.unblockErrorMessage)
        port.unblockResult = Result.Success(Unit)
        actions.unblock(target)
        runCurrent()
        assertFalse(state.value.block.isBlocked(target.userId))
        assertNull(state.value.block.unblockErrorMessage)
    }

    @Test
    fun oldStatusCannotUndoSuccessfulBlock() = runTest {
        val pending = CompletableDeferred<Result<Boolean>>()
        val port = BlockerFake().apply { statusCall = { pending.await() } }
        val state = MutableStateFlow(MessagesUiState())
        val actions = ConversationBlockActions(state, port, backgroundScope)
        actions.refreshStatus(target.userId)
        runCurrent()
        actions.request(target)
        actions.confirm()
        runCurrent()
        pending.complete(Result.Success(false))
        runCurrent()
        assertTrue(state.value.block.isBlocked(target.userId))
    }

    @Test
    fun lateStatusFromPreviousViewerCannotPopulateNewViewerState() = runTest {
        val pending = CompletableDeferred<Result<Boolean>>()
        val port = BlockerFake().apply { statusCall = { withContext(NonCancellable) { pending.await() } } }
        val state = MutableStateFlow(MessagesUiState())
        val actions = ConversationBlockActions(state, port, backgroundScope)
        actions.refreshStatus(target.userId)
        runCurrent()
        actions.reset()
        state.value = MessagesUiState()
        pending.complete(Result.Success(true))
        runCurrent()
        assertTrue(state.value.block.blockedUserIds.isEmpty())
        assertTrue(state.value.block.checkedUserIds.isEmpty())
    }

    @Test
    fun statusFailureIsVisibleAndRetryLoadsPersistedBlock() = runTest {
        val port = BlockerFake().apply { statusCall = { throw IllegalStateException() } }
        val state = MutableStateFlow(MessagesUiState())
        val actions = ConversationBlockActions(state, port, backgroundScope)
        actions.refreshStatus(target.userId)
        runCurrent()
        assertNotNull(state.value.block.statusErrorMessage)
        assertTrue(state.value.block.checkedUserIds.isEmpty())
        port.statusCall = { Result.Success(true) }
        actions.refreshStatus(target.userId)
        runCurrent()
        assertTrue(state.value.block.isBlocked(target.userId))
        assertNull(state.value.block.statusErrorMessage)
    }
}

private class BlockerFake : ConversationUserBlocker {
    var blocks = 0
    var blockCall: suspend () -> Result<Unit> = { Result.Success(Unit) }
    var statusCall: suspend () -> Result<Boolean> = { Result.Success(false) }
    var unblockResult: Result<Unit> = Result.Success(Unit)
    override suspend fun isBlocked(userId: SenderId) = statusCall()
    override suspend fun block(userId: SenderId): Result<Unit> {
        blocks++
        return blockCall()
    }
    override suspend fun unblock(userId: SenderId) = unblockResult
}
