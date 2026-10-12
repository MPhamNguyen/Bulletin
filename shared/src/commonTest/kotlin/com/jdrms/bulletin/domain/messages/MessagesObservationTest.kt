package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.ObserveConversationMessages
import com.jdrms.bulletin.domain.messages.domain.model.ConversationAccessException
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.repository.MessageStreamEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class MessagesObservationTest {
    @Test
    fun participantReceivesOnlySelectedConversationInserts() = runTest {
        val otherId = ConversationId("other")
        val repository = messagesRepository(listOf(conversation(), conversation(otherId)))
        val events = mutableListOf<MessageStreamEvent>()
        val job = backgroundScope.launch {
            repository.observe(alice.id, conversationId).take(2).toList(events)
        }
        runCurrent()

        repository.sendMessage(message(inConversation = otherId, id = MessageId("other-message")))
        repository.sendMessage(message())
        runCurrent()

        assertEquals(listOf(MessageStreamEvent.Connected, MessageStreamEvent.Received(message())), events)
        job.cancel()
    }

    @Test
    fun outsiderCannotObserveConversation() = runTest {
        val event = messagesRepository().observe(bob.id, conversationId).first()
        assertIs<ConversationAccessException>(assertIs<MessageStreamEvent.Failed>(event).cause)
    }

    @Test
    fun useCaseDoesNotOpenStreamWithoutAuthenticatedSender() = runTest {
        val repository = messagesRepository()
        val provider = MutableMessageSenderProvider(Result.Error(ConversationAccessException()))
        val event = ObserveConversationMessages(repository, provider)(conversationId).first()
        assertIs<ConversationAccessException>(assertIs<MessageStreamEvent.Failed>(event).cause)
    }
}
