package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.domain.messages.application.MessageSenderLookupException
import com.jdrms.bulletin.domain.messages.application.MessagingAuthenticationRequiredException
import com.jdrms.bulletin.domain.messages.domain.model.ConversationAccessException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class MessageSendingIdentityFailureTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun authenticationFailureRequiresSignInAndClearsSessionDrafts() = runTest {
        assertIdentityFailure(MessagingAuthenticationRequiredException(), "Sign in to access your messages.")
    }

    @Test
    fun senderLookupFailureClearsMessagingState() = runTest {
        assertIdentityFailure(
            MessageSenderLookupException(IllegalStateException("Private auth detail")),
            "Unable to complete the messaging request. Please try again."
        )
    }

    @Test
    fun accessFailureClearsMessagingState() = runTest {
        assertIdentityFailure(
            ConversationAccessException(),
            "Unable to complete the messaging request. Please try again."
        )
    }

    private fun TestScope.assertIdentityFailure(exception: Exception, expectedMessage: String) {
        val pending = PendingMessageSends()
        val provider = MutableMessageSenderProvider()
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
        pending.fail(0, exception)
        advanceUntilIdle()
        assertEquals(expectedMessage, viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.selectedConversationId)
        assertEquals(emptyList(), viewModel.uiState.value.currentMessages)
        assertEquals("", viewModel.uiState.value.messageInput)
        assertEquals(emptyMap(), viewModel.uiState.value.sendStates)
        assertFalse(viewModel.uiState.value.isSending)
        pending.succeed(1)
        advanceUntilIdle()
        assertEquals(expectedMessage, viewModel.uiState.value.errorMessage)
        assertEquals(emptyList(), viewModel.uiState.value.currentMessages)
        assertEquals(emptyMap(), viewModel.uiState.value.sendStates)
        viewModel.selectConversation(conversationId)
        advanceUntilIdle()
        assertEquals("", viewModel.uiState.value.messageInput)
        viewModel.selectConversation(secondDraftConversationId)
        advanceUntilIdle()
        assertEquals("", viewModel.uiState.value.messageInput)
    }
}
