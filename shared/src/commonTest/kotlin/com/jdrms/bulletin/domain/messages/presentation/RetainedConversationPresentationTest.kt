package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.MutableMessageSenderProvider
import com.jdrms.bulletin.domain.messages.alice
import com.jdrms.bulletin.domain.messages.carol
import com.jdrms.bulletin.domain.messages.conversation
import com.jdrms.bulletin.domain.messages.conversationId
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import com.jdrms.bulletin.domain.messages.draftViewModel
import com.jdrms.bulletin.domain.messages.message
import com.jdrms.bulletin.domain.messages.messagesRepository
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
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RetainedConversationPresentationTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun retainedThreadLoadsHistoryButSendAndEditActionsAreUnavailable() = runTest {
        val retained = message(carol).copy(senderId = null)
        val thread = conversation(senders = listOf(alice)).recordMessage(retained).getOrThrow()
        val fixture = RetainedConversationFixture(listOf(thread), listOf(retained))
        val viewModel = draftViewModel(fixture.repository, MutableMessageSenderProvider(), dispatcher)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(listOf(retained.id), state.currentMessages.map { it.id })
        assertEquals("User Not Found", state.currentMessages.single().senderName)
        assertEquals("User Not Found", conversationTitle(state.conversations.single(), alice.id))
        assertTrue(state.isSelectedConversationReadOnly)
        assertFalse(state.canSendToCounterpart())
        viewModel.onMessageInputChanged("Must not send")
        viewModel.sendCurrentMessage()
        viewModel.edit(retained.id)
        advanceUntilIdle()
        assertEquals(0, fixture.sendCalls)
        assertIs<MessageEditState.Composing>(viewModel.uiState.value.editState)
        viewModel.report(retained.id)
        assertEquals(retained.id, viewModel.uiState.value.messageReportForm?.messageId)
    }

    @Test
    fun deletionDetachesCachedAuthorWithoutLosingHistoryOrDraft() = runTest {
        val received = message(carol)
        val own = message(id = MessageId("own")).copy(timestampMillis = 43L)
        val fixture = RetainedConversationFixture(listOf(conversation()), listOf(received, own))
        val viewModel = draftViewModel(fixture.repository, MutableMessageSenderProvider(), dispatcher)
        advanceUntilIdle()
        viewModel.onMessageInputChanged("Unsent draft")
        fixture.conversations = listOf(conversation(senders = listOf(alice)).recordMessage(own).getOrThrow())
        viewModel.loadConversations()
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(listOf(received.id, own.id), state.currentMessages.map { it.id })
        assertNull(state.currentMessages.first().senderId)
        assertEquals("User Not Found", state.currentMessages.first().senderName)
        assertIs<MessageEditState.Composing>(state.editState)
        assertEquals("Unsent draft", state.messageInput)
        viewModel.sendCurrentMessage()
        advanceUntilIdle()
        assertEquals(0, fixture.sendCalls)
        assertTrue(state.isSelectedConversationReadOnly)
    }

    @Test
    fun membershipRefreshCancelsEditingAndRestoresTheUnsentDraft() {
        val own = message()
        val state = MessagesUiState(
            selectedConversationId = conversationId,
            currentMessages = listOf(own, message(carol, id = MessageId("received"))),
            editState = MessageEditState.Editing(MessageEditSession(own, alice.id, "Unsent draft")),
            messageInput = "An edit in progress"
        ).withRetainedHistory(conversation(senders = listOf(alice)))
        assertIs<MessageEditState.Composing>(state.editState)
        assertEquals("Unsent draft", state.messageInput)
        assertNull(state.currentMessages.last().senderId)
    }

    @Test
    fun anonymousHistoryDisablesSendBeforeMembershipRefreshFinishes() {
        val state = MessagesUiState(
            viewerId = alice.id,
            conversations = listOf(conversation()),
            selectedConversationId = conversationId,
            currentMessages = listOf(message(carol).copy(senderId = null))
        )
        assertTrue(state.isSelectedConversationReadOnly)
        assertFalse(state.canSendToCounterpart())
        assertFalse(state.copy(viewerId = null).canSendToCounterpart())
    }
}

private class RetainedConversationFixture(conversations: List<Conversation>, messages: List<Message>) {
    var conversations = conversations
    var sendCalls = 0
        private set
    private val backing = messagesRepository(conversations, messages)
    val repository = object : MessagesRepository by backing {
        override suspend fun getConversations(userId: SenderId): Result<List<Conversation>> =
            Result.Success(this@RetainedConversationFixture.conversations)

        override suspend fun sendMessage(message: Message): Result<Message> {
            sendCalls++
            return backing.sendMessage(message)
        }
    }
}
