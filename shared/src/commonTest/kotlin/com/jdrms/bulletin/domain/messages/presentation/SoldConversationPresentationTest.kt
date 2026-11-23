package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.MutableMessageSenderProvider
import com.jdrms.bulletin.domain.messages.alice
import com.jdrms.bulletin.domain.messages.carol
import com.jdrms.bulletin.domain.messages.conversation
import com.jdrms.bulletin.domain.messages.conversationId
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationLockReason
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import com.jdrms.bulletin.domain.messages.domain.model.Message
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SoldConversationPresentationTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun saleLocksSendingUntilListingIsRestored() = runTest {
        val fixture = SoldConversationFixture()
        val viewModel = draftViewModel(fixture.repository, MutableMessageSenderProvider(), dispatcher)
        advanceUntilIdle()
        viewModel.onMessageInputChanged("Unsent draft")

        fixture.isSold = true
        viewModel.loadConversations()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ConversationLockReason.LISTING_SOLD, state.selectedConversationLockReason)
        assertTrue(state.selectedConversationLockReason!!.description.contains("sold"))
        assertFalse(state.canSendToCounterpart())
        assertIs<MessageEditState.Composing>(state.editState)
        assertEquals("Unsent draft", state.messageInput)
        assertEquals(carol.displayName, conversationTitle(state.conversations.single(), alice.id))
        assertEquals(listOf(message().id), state.currentMessages.map { it.id })
        viewModel.sendCurrentMessage()
        viewModel.edit(message().id)
        advanceUntilIdle()
        assertEquals(0, fixture.sendCalls)
        assertIs<MessageEditState.Composing>(viewModel.uiState.value.editState)

        fixture.isSold = false
        viewModel.loadConversations()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.canSendToCounterpart())
        assertEquals("Unsent draft", viewModel.uiState.value.messageInput)
    }

    @Test
    fun saleWhileEditingRestoresDraftAndKeepsMessageAuthors() {
        val own = message()
        val sold = Conversation(conversationId, conversation().participants, isListingSold = true)
        val state = MessagesUiState(
            conversations = listOf(sold),
            selectedConversationId = conversationId,
            currentMessages = listOf(own),
            editState = MessageEditState.Editing(MessageEditSession(own, alice.id, "Unsent draft")),
            messageInput = "An edit in progress"
        ).withRetainedHistory(sold)

        assertIs<MessageEditState.Composing>(state.editState)
        assertEquals("Unsent draft", state.messageInput)
        assertEquals(alice.id, state.currentMessages.single().senderId)
        assertEquals(ConversationLockReason.LISTING_SOLD, state.selectedConversationLockReason)
    }
}

private class SoldConversationFixture {
    var isSold = false
    var sendCalls = 0
    private val backing = messagesRepository(messages = listOf(message()))
    val repository = object : MessagesRepository by backing {
        override suspend fun getConversations(userId: SenderId): Result<List<Conversation>> = Result.Success(
            listOf(
                Conversation(
                    id = conversationId,
                    participants = conversation().participants,
                    listingId = ListingReferenceId("listing"),
                    isListingSold = isSold
                )
            )
        )

        override suspend fun sendMessage(message: Message): Result<Message> {
            sendCalls++
            return backing.sendMessage(message)
        }
    }
}
