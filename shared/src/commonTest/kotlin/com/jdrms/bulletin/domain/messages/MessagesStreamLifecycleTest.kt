package com.jdrms.bulletin.domain.messages

import androidx.lifecycle.ViewModelStore
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.GetConversationMessages
import com.jdrms.bulletin.domain.messages.application.HideConversation
import com.jdrms.bulletin.domain.messages.application.MessageSender
import com.jdrms.bulletin.domain.messages.application.ObserveConversationMessages
import com.jdrms.bulletin.domain.messages.application.ReportMessage
import com.jdrms.bulletin.domain.messages.application.SendMessage
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.repository.MessageStreamEvent
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesObserver
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import com.jdrms.bulletin.domain.messages.presentation.MessageConnectionState
import com.jdrms.bulletin.domain.messages.presentation.MessagesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MessagesStreamLifecycleTest {
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
    fun clearingViewModelCancelsConversationStream() = runTest {
        var streamCancelled = false
        val observer = MessagesObserver { _, _ ->
            flow {
                emit(MessageStreamEvent.Connected)
                try {
                    awaitCancellation()
                } finally {
                    streamCancelled = true
                }
            }
        }
        val viewModel = viewModel(messagesRepository(), observer)
        val store = ViewModelStore()
        store.put("messages", viewModel)
        viewModel.onDestinationActiveChanged(true)

        advanceUntilIdle()
        assertEquals(MessageConnectionState.CONNECTED, viewModel.uiState.value.connectionState)

        store.clear()
        runCurrent()

        assertTrue(streamCancelled)
    }

    @Test
    fun leavingAndReopeningMessagesCancelsAndRestartsConversationStream() = runTest {
        var starts = 0
        var cancellations = 0
        val observer = MessagesObserver { _, _ ->
            flow {
                starts++
                emit(MessageStreamEvent.Connected)
                try {
                    awaitCancellation()
                } finally {
                    cancellations++
                }
            }
        }
        val viewModel = viewModel(messagesRepository(), observer)
        viewModel.onDestinationActiveChanged(true)

        advanceUntilIdle()
        assertEquals(1, starts)
        viewModel.onDestinationActiveChanged(false)
        runCurrent()
        assertEquals(1, cancellations)

        viewModel.onDestinationActiveChanged(true)
        advanceUntilIdle()

        assertEquals(2, starts)
        assertEquals(1, cancellations)
        assertEquals(MessageConnectionState.CONNECTED, viewModel.uiState.value.connectionState)
    }

    @Test
    fun reconnectingStreamShowsConnectingStateWithoutFailureBanner() = runTest {
        val events = MutableSharedFlow<MessageStreamEvent>()
        val observer = MessagesObserver { _, _ -> events }
        val viewModel = viewModel(messagesRepository(), observer)
        viewModel.onDestinationActiveChanged(true)
        advanceUntilIdle()

        events.emit(MessageStreamEvent.Connected)
        runCurrent()
        assertEquals(MessageConnectionState.CONNECTED, viewModel.uiState.value.connectionState)

        events.emit(MessageStreamEvent.Reconnecting)
        runCurrent()
        assertEquals(MessageConnectionState.CONNECTING, viewModel.uiState.value.connectionState)

        events.emit(MessageStreamEvent.Disconnected)
        runCurrent()
        assertEquals(MessageConnectionState.DISCONNECTED, viewModel.uiState.value.connectionState)
    }

    @Test
    fun realtimeMessagesResolveSenderNamesAndFallbackEchoKeepsHydratedName() = runTest {
        val ownSender = MessageSender(alice.id, "Alice Profile")
        val otherSender = MessageSender(carol.id, "Carol Profile")
        provider.result = Result.Success(ownSender)
        val participants = listOf(
            ConversationParticipant(ownSender.id, ownSender.displayName),
            ConversationParticipant(otherSender.id, otherSender.displayName)
        )
        val repository = messagesRepository(listOf(Conversation(conversationId, participants)))
        val events = MutableSharedFlow<MessageStreamEvent>()
        val viewModel = viewModel(repository, MessagesObserver { _, _ -> events })
        viewModel.onDestinationActiveChanged(true)
        advanceUntilIdle()

        viewModel.onMessageInputChanged("Hello")
        viewModel.sendCurrentMessage()
        advanceUntilIdle()
        assertEquals("Alice Profile", viewModel.uiState.value.currentMessages.single().senderName)

        events.emit(
            MessageStreamEvent.Received(
                message(sender = alice, id = MessageId("new-message")).copy(senderName = "Student")
            )
        )
        runCurrent()
        events.emit(
            MessageStreamEvent.Received(
                message(sender = carol, id = MessageId("incoming")).copy(senderName = "Student")
            )
        )
        runCurrent()

        assertEquals(
            mapOf(MessageId("new-message") to "Alice Profile", MessageId("incoming") to "Carol Profile"),
            viewModel.uiState.value.currentMessages.associate { it.id to it.senderName }
        )
    }

    private fun viewModel(
        repository: MessagesRepository,
        observer: MessagesObserver = repository as? MessagesObserver ?: messagesRepository()
    ) = MessagesViewModel(
        testInboxOwner(repository, provider, dispatcher),
        GetConversationMessages(repository, provider),
        ObserveConversationMessages(observer, provider),
        SendMessage(repository, provider, nextMessageId = { MessageId("new-message") }, nowMillis = { 123L }),
        ReportMessage(repository, provider),
        HideConversation(repository, provider)
    )
}
