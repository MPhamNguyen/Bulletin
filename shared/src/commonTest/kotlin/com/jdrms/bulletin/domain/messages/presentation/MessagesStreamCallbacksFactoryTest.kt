package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.MutableMessageSenderProvider
import com.jdrms.bulletin.domain.messages.conversationId
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import com.jdrms.bulletin.domain.messages.message
import com.jdrms.bulletin.domain.messages.messagesRepository
import com.jdrms.bulletin.domain.messages.testInboxOwner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MessagesStreamCallbacksFactoryTest {
    @Test
    fun messageBeforeInboxConversationIsDeliveredOnceAfterSnapshotArrives() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val provider = MutableMessageSenderProvider()
        val backing = messagesRepository()
        var conversationVisible = false
        val repository = object : MessagesRepository by backing {
            override suspend fun getConversations(userId: SenderId): Result<List<Conversation>> =
                if (conversationVisible) backing.getConversations(userId) else Result.Success(emptyList())
        }
        val inbox = testInboxOwner(repository, provider, dispatcher)
        runCurrent()
        val uiState = MutableStateFlow(MessagesUiState(selectedConversationId = conversationId))
        var reloadCount = 0
        val factory = MessagesStreamCallbacksFactory(
            uiState,
            inbox,
            { reloadCount++ },
            { },
            { false }
        )
        val callbacks = factory.forConversation(conversationId)

        callbacks.onReceived(message())
        runCurrent()
        factory.onInboxSnapshot(inbox.state.value)
        assertEquals(emptyList(), uiState.value.currentMessages)
        assertEquals(0, reloadCount)
        assertFalse(inbox.state.value.conversations.any { it.id == conversationId })

        conversationVisible = true
        inbox.refresh()
        runCurrent()
        factory.onInboxSnapshot(inbox.state.value)
        factory.onInboxSnapshot(inbox.state.value)

        assertEquals(listOf(message()), uiState.value.currentMessages)
        assertEquals(message(), inbox.state.value.conversations.single().lastMessage)
        assertEquals(1, reloadCount)
        inbox.close()
    }

    @Test
    fun pendingMessageIsClearedWhenViewerChanges() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val provider = MutableMessageSenderProvider()
        val inbox = testInboxOwner(messagesRepository(conversations = emptyList()), provider, dispatcher)
        runCurrent()
        val uiState = MutableStateFlow(MessagesUiState(selectedConversationId = conversationId))
        val factory = MessagesStreamCallbacksFactory(uiState, inbox, { }, { }, { false })

        factory.forConversation(conversationId).onReceived(message())
        assertTrue(factory.hasPendingFor(conversationId))
        provider.result = Result.Error(IllegalStateException("signed out"))
        runCurrent()
        factory.onInboxSnapshot(inbox.state.value)

        assertFalse(factory.hasPendingFor(conversationId))
        assertEquals(emptyList(), uiState.value.currentMessages)
        inbox.close()
    }
}
