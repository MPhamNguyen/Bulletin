package com.jdrms.bulletin.domain.messages

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class MessageSendingRecoveryTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun deliveredMessageWithConversationUpdateFailureCanRefreshWithoutResending() = runTest {
        val fixture = MessageSendingRecoveryFixture(dispatcher)
        val viewModel = fixture.viewModel
        advanceUntilIdle()
        viewModel.onMessageInputChanged("Delivered")
        viewModel.sendCurrentMessage()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isSending)
        assertNull(viewModel.uiState.value.sendErrorMessage)
        assertEquals(emptyMap(), viewModel.uiState.value.sendStates)
        assertEquals("", viewModel.uiState.value.messageInput)
        assertEquals(listOf("Delivered"), viewModel.uiState.value.currentMessages.map { it.content })
        assertEquals(
            "Message sent, but the conversation could not be updated.",
            viewModel.inboxState.value.errorMessage
        )
        assertEquals(viewModel.inboxState.value.errorMessage, viewModel.uiState.value.errorMessage)
        val readsBeforeRetry = fixture.inboxReads
        viewModel.loadConversations()
        advanceUntilIdle()
        assertEquals(readsBeforeRetry + 1, fixture.inboxReads)
        assertNull(viewModel.uiState.value.errorMessage)
        assertNull(viewModel.inboxState.value.errorMessage)
        assertEquals(listOf("Delivered"), viewModel.uiState.value.currentMessages.map { it.content })
        assertEquals("", viewModel.uiState.value.messageInput)
        assertEquals(1, fixture.sendCount)
    }

    @Test
    fun failedConversationRefreshKeepsSendSuccessfulAndAllowsAnotherRefresh() = runTest {
        val fixture = MessageSendingRecoveryFixture(dispatcher)
        val viewModel = fixture.viewModel
        advanceUntilIdle()
        viewModel.onMessageInputChanged("Delivered")
        viewModel.sendCurrentMessage()
        advanceUntilIdle()
        val readsBeforeRetry = fixture.inboxReads
        fixture.refreshFails = true
        viewModel.loadConversations()
        advanceUntilIdle()
        assertEquals(readsBeforeRetry + 1, fixture.inboxReads)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.sendErrorMessage)
        assertEquals(emptyMap(), viewModel.uiState.value.sendStates)
        fixture.refreshFails = false
        viewModel.loadConversations()
        advanceUntilIdle()
        assertEquals(readsBeforeRetry + 2, fixture.inboxReads)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(1, fixture.sendCount)
    }
}
