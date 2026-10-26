package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.MutableMessageSenderProvider
import com.jdrms.bulletin.domain.messages.alice
import com.jdrms.bulletin.domain.messages.application.EditMessage
import com.jdrms.bulletin.domain.messages.application.GetConversations
import com.jdrms.bulletin.domain.messages.application.ObserveInboxInvalidations
import com.jdrms.bulletin.domain.messages.carol
import com.jdrms.bulletin.domain.messages.conversation
import com.jdrms.bulletin.domain.messages.conversationId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageEdit
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.repository.InboxObserver
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import com.jdrms.bulletin.domain.messages.message
import com.jdrms.bulletin.domain.messages.messagesRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class MessageEditingActionsTest {
    private val original = message()
    private val now = original.orderingTimestamp + 1.minutes

    @Test
    fun cancelAndSaveRestoreUnsentDraftAndSaveDoesNotInsertAMessage() = runTest {
        val fixture = fixture()
        fixture.actions.begin(original.id)
        assertEquals(original.content, fixture.state.value.messageInput)
        fixture.state.value = fixture.state.value.copy(messageInput = "Discard this")
        fixture.actions.cancel()
        assertEquals("Unsent draft", fixture.state.value.messageInput)
        fixture.actions.begin(original.id)
        fixture.state.value = fixture.state.value.copy(messageInput = "Updated")
        fixture.actions.save()
        fixture.actions.save()
        runCurrent()
        assertEquals(MessageEditState.Composing, fixture.state.value.editState)
        assertEquals("Unsent draft", fixture.state.value.messageInput)
        assertEquals("Updated", fixture.state.value.currentMessages.single().content)
        assertEquals(1, fixture.repository.getMessagePage(alice.id, conversationId).getOrThrow().messages.size)
    }

    @Test
    fun failureKeepsReplacementAndAllowsRetry() = runTest {
        val backing = messagesRepository(messages = listOf(original), now = { now })
        val repository = object : MessagesRepository by backing {
            override suspend fun editMessage(userId: SenderId, edit: MessageEdit): Result<Message> =
                Result.Error(IllegalStateException("Offline"))
        }
        val fixture = fixture(repository)
        fixture.actions.begin(original.id)
        fixture.state.value = fixture.state.value.copy(messageInput = "Keep this text")
        fixture.actions.save()
        runCurrent()
        assertIs<MessageEditState.Editing>(fixture.state.value.editState)
        assertEquals("Keep this text", fixture.state.value.messageInput)
        assertNotNull(fixture.state.value.editError)
        assertEquals(original, fixture.state.value.currentMessages.single())
    }

    @Test
    fun pendingSaveCannotReplaceAnotherConversationOrNewSessionState() = runTest {
        val completion = CompletableDeferred<Result<Message>>()
        val backing = messagesRepository(messages = listOf(original), now = { now })
        val repository = object : MessagesRepository by backing {
            override suspend fun editMessage(userId: SenderId, edit: MessageEdit): Result<Message> = completion.await()
        }
        val fixture = fixture(repository)
        fixture.actions.begin(original.id)
        fixture.actions.save()
        runCurrent()
        assertIs<MessageEditState.Saving>(fixture.state.value.editState)
        fixture.state.value = MessagesUiState(messageInput = "Another conversation draft")
        fixture.provider.result = Result.Success(carol)
        completion.complete(Result.Success(original.copy(content = "Late edit", editedAt = now)))
        runCurrent()
        assertEquals("Another conversation draft", fixture.state.value.messageInput)
        assertEquals(emptyList(), fixture.state.value.currentMessages)
    }

    private fun TestScope.fixture(
        repository: MessagesRepository = messagesRepository(messages = listOf(original), now = { now })
    ): Fixture {
        val provider = MutableMessageSenderProvider()
        val owner = InboxStateOwner(
            GetConversations(repository, provider),
            ObserveInboxInvalidations(InboxObserver { emptyFlow() }),
            provider,
            backgroundScope
        )
        runCurrent()
        val state = MutableStateFlow(
            MessagesUiState(
                viewerId = alice.id,
                conversations = listOf(conversation()),
                selectedConversationId = conversationId,
                currentMessages = listOf(original),
                messageInput = "Unsent draft"
            )
        )
        val actions = MessageEditingActions(
            owner,
            state,
            EditMessage(repository, provider),
            backgroundScope,
            now = { now }
        )
        return Fixture(repository, provider, state, actions)
    }

    private data class Fixture(
        val repository: MessagesRepository,
        val provider: MutableMessageSenderProvider,
        val state: MutableStateFlow<MessagesUiState>,
        val actions: MessageEditingActions
    )
}
