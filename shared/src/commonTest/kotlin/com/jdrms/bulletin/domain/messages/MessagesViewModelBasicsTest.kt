package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.GetConversationMessages
import com.jdrms.bulletin.domain.messages.application.HideConversation
import com.jdrms.bulletin.domain.messages.application.ObserveConversationMessages
import com.jdrms.bulletin.domain.messages.application.ReportMessage
import com.jdrms.bulletin.domain.messages.application.SendMessage
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesObserver
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import com.jdrms.bulletin.domain.messages.presentation.MessagesViewModel
import com.jdrms.bulletin.domain.messages.presentation.conversationTitle
import com.jdrms.bulletin.domain.messages.presentation.shouldLoadOlderAfterScroll
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MessagesViewModelBasicsTest {
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
    fun previewTitleShowsOnlyOtherParticipantAndHasSafeEmptyFallback() {
        assertEquals("Carol", conversationTitle(conversation(), alice.id))
        assertEquals("Conversation", conversationTitle(conversation(), null))
        assertEquals("Conversation", conversationTitle(conversation(senders = listOf(alice)), alice.id))
    }

    @Test
    fun loadsOnlyAuthenticatedParticipantsConversationsAndMessages() = runTest {
        val repository = messagesRepository(
            listOf(conversation(), conversation(ConversationId("other"), listOf(bob, carol))),
            listOf(message())
        )
        val viewModel = viewModel(repository)
        assertTrue(viewModel.uiState.value.isLoading)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(listOf(conversationId), state.conversations.map { it.id })
        assertEquals(alice.id, state.viewerId)
        assertEquals(
            listOf(carol.displayName),
            state.conversations.single().otherParticipantNames(assertNotNull(state.viewerId))
        )
        assertEquals(listOf(message()), state.currentMessages)
        assertEquals(conversationId, state.selectedConversationId)
        assertFalse(state.isLoading)
        assertFalse(state.isLoadingMessages)
    }

    @Test
    fun openingConversationAfterSendSelectsTheRequestedThread() = runTest {
        val existing = conversation()
        val requested = conversation(ConversationId("requested"))
        val viewModel = viewModel(messagesRepository(listOf(existing, requested)))
        advanceUntilIdle()
        viewModel.openConversation(requested.id)
        advanceUntilIdle()
        assertEquals(requested.id, viewModel.uiState.value.selectedConversationId)
    }

    @Test
    fun removingSelectedConversationKeepsOtherConversationAndSelectsIt() = runTest {
        val other = conversation(ConversationId("other"))
        val repository = messagesRepository(listOf(conversation(), other), listOf(message()))
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.hideConversationFromInbox(conversationId)
        advanceUntilIdle()

        assertEquals(listOf(other.id), viewModel.uiState.value.conversations.map { it.id })
        assertEquals(other.id, viewModel.uiState.value.selectedConversationId)
        assertEquals(emptyList(), repository.getConversations(alice.id).getOrThrow().filter { it.id == conversationId })
        assertTrue(repository.getConversations(carol.id).getOrThrow().any { it.id == conversationId })
        assertEquals(listOf(message()), repository.getMessages(carol.id, conversationId).getOrThrow())
    }

    @Test
    fun removingUnselectedConversationKeepsCurrentSelection() = runTest {
        val other = conversation(ConversationId("other"))
        val viewModel = viewModel(messagesRepository(listOf(conversation(), other)))
        advanceUntilIdle()

        viewModel.hideConversationFromInbox(other.id)
        advanceUntilIdle()

        assertEquals(listOf(conversationId), viewModel.uiState.value.conversations.map { it.id })
        assertEquals(conversationId, viewModel.uiState.value.selectedConversationId)
    }

    @Test
    fun failedRemovalLeavesConversationVisibleWithError() = runTest {
        val backing = messagesRepository()
        val repository = object : MessagesRepository by backing {
            override suspend fun hideConversation(userId: SenderId, conversationId: ConversationId): Result<Unit> =
                Result.Error(IllegalStateException("Storage unavailable"))
        }
        val viewModel = viewModel(repository, backing)
        advanceUntilIdle()

        viewModel.hideConversationFromInbox(conversationId)
        advanceUntilIdle()

        assertEquals(listOf(conversationId), viewModel.uiState.value.conversations.map { it.id })
        assertEquals(conversationId, viewModel.uiState.value.selectedConversationId)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun sendUsesAuthenticatedIdentityAndRefreshesSelectedConversation() = runTest {
        val repository = messagesRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.onMessageInputChanged(" Hello ")
        viewModel.sendCurrentMessage()
        advanceUntilIdle()
        val sent = repository.getMessages(alice.id, conversationId).getOrThrow().single()
        assertEquals(alice.id, sent.senderId)
        assertEquals(alice.displayName, sent.senderName)
        assertEquals("Hello", sent.content)
        assertEquals("", viewModel.uiState.value.messageInput)
        assertEquals(conversationId, viewModel.uiState.value.selectedConversationId)
        assertEquals(listOf(sent), viewModel.uiState.value.currentMessages)
        assertNull(viewModel.uiState.value.statusMessage)
        assertFalse(viewModel.uiState.value.isSending)
    }

    @Test
    fun olderHistoryLoadsOnceAndKeepsChronologicalOrder() = runTest {
        val history = (1..35).map { index ->
            message(id = MessageId("message-$index")).copy(timestampMillis = index.toLong())
        }
        val viewModel = viewModel(messagesRepository(messages = history))
        advanceUntilIdle()
        assertEquals(30, viewModel.uiState.value.currentMessages.size)
        assertNotNull(viewModel.uiState.value.olderCursor)
        viewModel.loadOlderMessages()
        viewModel.loadOlderMessages()
        advanceUntilIdle()
        assertEquals(history, viewModel.uiState.value.currentMessages)
        assertNull(viewModel.uiState.value.olderCursor)
        assertFalse(viewModel.uiState.value.isLoadingOlder)
    }

    @Test
    fun olderHistoryLoadsOnlyAfterEnteringTopThresholdFromBelow() {
        assertFalse(shouldLoadOlderAfterScroll(null, 0))
        assertFalse(shouldLoadOlderAfterScroll(0, 0))
        assertFalse(shouldLoadOlderAfterScroll(1, 1))
        assertTrue(shouldLoadOlderAfterScroll(1, 0))
    }

    @Test
    fun streamInsertAndSendResponseAppearOnce() = runTest {
        val repository = messagesRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.onMessageInputChanged("Hello")
        viewModel.sendCurrentMessage()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.currentMessages.size)
        assertEquals(MessageId("new-message"), viewModel.uiState.value.currentMessages.single().id)
    }

    @Test
    fun switchingConversationStopsOldStreamDelivery() = runTest {
        val other = conversation(ConversationId("other"))
        val repository = messagesRepository(listOf(conversation(), other))
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.selectConversation(other.id)
        advanceUntilIdle()
        repository.sendMessage(message())
        val otherMessage = message(inConversation = other.id, id = MessageId("other-message"))
        repository.sendMessage(otherMessage)
        advanceUntilIdle()
        assertEquals(listOf(otherMessage), viewModel.uiState.value.currentMessages)
    }

    private fun viewModel(
        repository: MessagesRepository,
        observer: MessagesObserver = repository as? MessagesObserver ?: messagesRepository()
    ): MessagesViewModel = MessagesViewModel(
        testInboxOwner(repository, provider, dispatcher),
        GetConversationMessages(repository, provider),
        ObserveConversationMessages(observer, provider),
        SendMessage(repository, provider, nextMessageId = { MessageId("new-message") }, nowMillis = { 123L }),
        ReportMessage(repository, provider),
        HideConversation(repository, provider)
    ).also { it.onDestinationActiveChanged(true) }
}
