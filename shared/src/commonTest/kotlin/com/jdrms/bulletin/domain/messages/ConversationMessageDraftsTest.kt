package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.MessagingAuthenticationRequiredException
import com.jdrms.bulletin.domain.messages.presentation.MessageEditState
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
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationMessageDraftsTest {
    private val dispatcher = StandardTestDispatcher()
    private val provider = MutableMessageSenderProvider()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun switchingConversationsRestoresTheirOwnDraftsIncludingWhitespace() = runTest {
        val viewModel = draftViewModel(draftRepository(), provider, dispatcher)
        advanceUntilIdle()
        viewModel.selectConversation(conversationId)
        viewModel.onMessageInputChanged(" Draft A\n ")
        viewModel.selectConversation(secondDraftConversationId)
        assertEquals("", viewModel.uiState.value.messageInput)
        viewModel.onMessageInputChanged("Draft B")
        viewModel.selectConversation(conversationId)
        assertEquals(" Draft A\n ", viewModel.uiState.value.messageInput)
        viewModel.selectConversation(secondDraftConversationId)
        assertEquals("Draft B", viewModel.uiState.value.messageInput)
    }

    @Test
    fun reopeningFromInboxOrListingAndReselectingKeepsTheDraft() = runTest {
        val viewModel = draftViewModel(draftRepository(), provider, dispatcher)
        advanceUntilIdle()
        viewModel.selectConversation(conversationId)
        viewModel.onMessageInputChanged("Keep this")
        viewModel.selectConversation(conversationId)
        assertEquals("Keep this", viewModel.uiState.value.messageInput)
        viewModel.returnToInbox()
        viewModel.openConversation(conversationId)
        advanceUntilIdle()
        assertEquals("Keep this", viewModel.uiState.value.messageInput)
    }

    @Test
    fun manuallyClearedDraftDoesNotReappearAfterSwitching() = runTest {
        val viewModel = draftViewModel(draftRepository(), provider, dispatcher)
        advanceUntilIdle()
        viewModel.selectConversation(conversationId)
        viewModel.onMessageInputChanged("Discard this")
        viewModel.selectConversation(secondDraftConversationId)
        viewModel.selectConversation(conversationId)
        viewModel.onMessageInputChanged("")
        viewModel.selectConversation(secondDraftConversationId)
        viewModel.selectConversation(conversationId)
        assertEquals("", viewModel.uiState.value.messageInput)
    }

    @Test
    fun switchingWhileEditingRestoresComposingDraftRatherThanReplacementText() = runTest {
        val original = message().copy(timestampMillis = Clock.System.now().toEpochMilliseconds())
        val repository = messagesRepository(
            listOf(conversation(), conversation(secondDraftConversationId)),
            listOf(original)
        )
        val viewModel = draftViewModel(repository, provider, dispatcher)
        advanceUntilIdle()
        viewModel.selectConversation(conversationId)
        advanceUntilIdle()
        viewModel.onMessageInputChanged("Unsent reply")
        viewModel.edit(original.id)
        assertEquals(original.content, viewModel.uiState.value.messageInput)
        viewModel.onMessageInputChanged("Replacement message text")
        viewModel.selectConversation(secondDraftConversationId)
        viewModel.selectConversation(conversationId)
        assertEquals(MessageEditState.Composing, viewModel.uiState.value.editState)
        assertEquals("Unsent reply", viewModel.uiState.value.messageInput)
    }

    @Test
    fun accountSwitchClearsActiveAndStoredDraftsEvenForSharedConversations() = runTest {
        val viewModel = draftViewModel(draftRepository(), provider, dispatcher)
        advanceUntilIdle()
        viewModel.selectConversation(conversationId)
        viewModel.onMessageInputChanged("Alice A")
        viewModel.selectConversation(secondDraftConversationId)
        viewModel.onMessageInputChanged("Alice B")
        provider.result = Result.Success(carol)
        viewModel.loadConversations()
        advanceUntilIdle()
        viewModel.selectConversation(conversationId)
        assertEquals("", viewModel.uiState.value.messageInput)
        viewModel.onMessageInputChanged("Carol A")
        viewModel.selectConversation(secondDraftConversationId)
        assertEquals("", viewModel.uiState.value.messageInput)
        provider.result = Result.Success(alice)
        viewModel.loadConversations()
        advanceUntilIdle()
        viewModel.selectConversation(conversationId)
        assertEquals("", viewModel.uiState.value.messageInput)
    }

    @Test
    fun signingOutAndBackInClearsAllDrafts() = runTest {
        val viewModel = draftViewModel(draftRepository(), provider, dispatcher)
        advanceUntilIdle()
        viewModel.selectConversation(conversationId)
        viewModel.onMessageInputChanged("Private A")
        viewModel.selectConversation(secondDraftConversationId)
        viewModel.onMessageInputChanged("Private B")
        provider.result = Result.Error(MessagingAuthenticationRequiredException())
        advanceUntilIdle()
        assertEquals("", viewModel.uiState.value.messageInput)
        provider.result = Result.Success(alice)
        advanceUntilIdle()
        viewModel.selectConversation(conversationId)
        assertEquals("", viewModel.uiState.value.messageInput)
        viewModel.selectConversation(secondDraftConversationId)
        assertEquals("", viewModel.uiState.value.messageInput)
    }
}
