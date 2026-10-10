package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.GetConversationMessages
import com.jdrms.bulletin.domain.messages.application.GetConversations
import com.jdrms.bulletin.domain.messages.application.MessageSenderLookupException
import com.jdrms.bulletin.domain.messages.application.ReportMessage
import com.jdrms.bulletin.domain.messages.application.SendMessage
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import com.jdrms.bulletin.domain.messages.presentation.MessagesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MessagesReportingViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val provider = MutableMessageSenderProvider()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun reportUsesSelectedConversationAndRefreshesReportedMessage() = runTest {
        val repository = messagesRepository(messages = listOf(message()))
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.onMessageInputChanged("Unsent draft")
        viewModel.report(message().id)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.currentMessages.single().isReported)
        assertEquals("Unsent draft", viewModel.uiState.value.messageInput)
        assertEquals("Message reported.", viewModel.uiState.value.statusMessage)
        assertFalse(viewModel.uiState.value.isReporting)
        assertEquals(emptySet(), viewModel.uiState.value.revealedReportedMessageIds)
    }

    @Test
    fun reportedMessagesCanBeShownAndHiddenIndividually() = runTest {
        val first = message(id = MessageId("first")).report()
        val second = message(id = MessageId("second")).report()
        val ordinary = message(id = MessageId("ordinary"))
        val viewModel = viewModel(messagesRepository(messages = listOf(first, second, ordinary)))
        advanceUntilIdle()
        assertEquals(emptySet(), viewModel.uiState.value.revealedReportedMessageIds)
        viewModel.toggleReportedMessageVisibility(first.id)
        assertEquals(setOf(first.id), viewModel.uiState.value.revealedReportedMessageIds)
        viewModel.toggleReportedMessageVisibility(second.id)
        assertEquals(setOf(first.id, second.id), viewModel.uiState.value.revealedReportedMessageIds)
        viewModel.toggleReportedMessageVisibility(first.id)
        assertEquals(setOf(second.id), viewModel.uiState.value.revealedReportedMessageIds)
        viewModel.toggleReportedMessageVisibility(ordinary.id)
        viewModel.toggleReportedMessageVisibility(MessageId("missing"))
        assertEquals(setOf(second.id), viewModel.uiState.value.revealedReportedMessageIds)
    }

    @Test
    fun revealingReportedMessageResetsWhenThreadIsReopened() = runTest {
        val reported = message().report()
        val viewModel = viewModel(
            messagesRepository(
                conversations = listOf(conversation(), conversation(ConversationId("other"))),
                messages = listOf(reported)
            )
        )
        advanceUntilIdle()
        viewModel.toggleReportedMessageVisibility(reported.id)
        assertEquals(setOf(reported.id), viewModel.uiState.value.revealedReportedMessageIds)
        viewModel.selectConversation(ConversationId("other"))
        assertEquals(emptySet(), viewModel.uiState.value.revealedReportedMessageIds)
        advanceUntilIdle()
        viewModel.selectConversation(conversationId)
        advanceUntilIdle()
        assertEquals(emptySet(), viewModel.uiState.value.revealedReportedMessageIds)
    }

    @Test
    fun expiredIdentityOnReportClearsStateAndDoesNotReport() = runTest {
        val repository = messagesRepository(messages = listOf(message()))
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        provider.result = Result.Error(MessageSenderLookupException(IllegalStateException("Private auth detail")))
        viewModel.report(message().id)
        advanceUntilIdle()
        assertFalse(repository.getMessages(alice.id, conversationId).getOrThrow().single().isReported)
        assertEquals(emptyList(), viewModel.uiState.value.currentMessages)
        assertEquals(
            "Unable to complete the messaging request. Please try again.",
            viewModel.uiState.value.errorMessage
        )
    }

    @Test
    fun reportingFailureExposesProgressAndKeepsMessagesVisible() = runTest {
        val backing = messagesRepository(messages = listOf(message()))
        val repository = object : MessagesRepository by backing {
            override suspend fun reportMessage(
                userId: SenderId,
                conversationId: ConversationId,
                messageId: MessageId,
                reason: String
            ): Result<Unit> {
                delay(1000)
                return Result.Error(IllegalStateException("Private transport detail"))
            }
        }
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.report(message().id)
        runCurrent()
        assertTrue(viewModel.uiState.value.isReporting)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isReporting)
        assertEquals(listOf(message()), viewModel.uiState.value.currentMessages)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    private fun viewModel(repository: MessagesRepository) = MessagesViewModel(
        GetConversations(repository, provider),
        GetConversationMessages(repository, provider),
        SendMessage(repository, provider, nextMessageId = { MessageId("new-message") }, nowMillis = { 123L }),
        ReportMessage(repository, provider)
    )
}
