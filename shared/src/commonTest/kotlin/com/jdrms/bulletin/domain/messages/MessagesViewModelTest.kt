package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.GetConversationMessages
import com.jdrms.bulletin.domain.messages.application.GetConversations
import com.jdrms.bulletin.domain.messages.application.MessageSenderLookupException
import com.jdrms.bulletin.domain.messages.application.MessagingAuthenticationRequiredException
import com.jdrms.bulletin.domain.messages.application.ReportMessage
import com.jdrms.bulletin.domain.messages.application.SendMessage
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.Message
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MessagesViewModelTest {
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
        assertEquals(listOf(message()), state.currentMessages)
        assertEquals(conversationId, state.selectedConversationId)
        assertFalse(state.isLoading)
        assertFalse(state.isLoadingMessages)
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
        assertEquals("Message sent.", viewModel.uiState.value.statusMessage)
        assertFalse(viewModel.uiState.value.isSending)
    }

    @Test
    fun sessionLossOnSendClearsPrivateStateAndDoesNotWrite() = runTest {
        val repository = messagesRepository(messages = listOf(message()))
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        provider.result = Result.Error(MessagingAuthenticationRequiredException())
        viewModel.onMessageInputChanged("Private draft")
        viewModel.sendCurrentMessage()
        advanceUntilIdle()
        assertEquals(1, repository.getMessages(alice.id, conversationId).getOrThrow().size)
        assertEquals(emptyList(), viewModel.uiState.value.conversations)
        assertEquals(emptyList(), viewModel.uiState.value.currentMessages)
        assertEquals("", viewModel.uiState.value.messageInput)
        assertEquals("Sign in to access your messages.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun accountSwitchCannotSendIntoPreviousUsersConversation() = runTest {
        val repository = messagesRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        provider.result = Result.Success(bob)
        viewModel.onMessageInputChanged("Hello")
        viewModel.sendCurrentMessage()
        advanceUntilIdle()
        assertEquals(emptyList(), repository.getMessages(alice.id, conversationId).getOrThrow())
        assertNull(viewModel.uiState.value.selectedConversationId)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun refreshingAfterAccountSwitchDropsOldSelectionMessagesAndDraft() = runTest {
        val other = conversation(ConversationId("other"), listOf(bob, carol))
        val viewModel = viewModel(messagesRepository(listOf(conversation(), other), listOf(message())))
        advanceUntilIdle()
        viewModel.onMessageInputChanged("Private draft")
        provider.result = Result.Success(bob)
        viewModel.loadConversations()
        assertEquals(emptyList(), viewModel.uiState.value.currentMessages)
        advanceUntilIdle()
        assertEquals(listOf(other.id), viewModel.uiState.value.conversations.map { it.id })
        assertEquals(other.id, viewModel.uiState.value.selectedConversationId)
        assertEquals("", viewModel.uiState.value.messageInput)
    }

    @Test
    fun oversizedMessageReturnsFailureWithoutCrashingOrWriting() = runTest {
        val repository = messagesRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.onMessageInputChanged("a".repeat(1001))
        viewModel.sendCurrentMessage()
        advanceUntilIdle()
        assertEquals(emptyList(), repository.getMessages(alice.id, conversationId).getOrThrow())
        assertEquals(1001, viewModel.uiState.value.messageInput.length)
        assertNotNull(viewModel.uiState.value.errorMessage)
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
    fun emptyInboxDoesNotSendReportOrSelectAnUnknownConversation() = runTest {
        val viewModel = viewModel(messagesRepository(emptyList()))
        advanceUntilIdle()
        viewModel.selectConversation(ConversationId("unknown"))
        viewModel.sendCurrentMessage()
        viewModel.report(MessageId("unknown"))
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.selectedConversationId)
        assertNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun storageReadFailureStopsLoadingWithoutExposingTechnicalDetails() = runTest {
        val repository = object : MessagesRepository by messagesRepository() {
            override suspend fun getConversations(userId: SenderId): Result<List<Conversation>> =
                Result.Error(IllegalStateException("Private transport detail"))
        }
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(
            "Unable to complete the messaging request. Please try again.",
            viewModel.uiState.value.errorMessage
        )
    }

    @Test
    fun retryAfterSignInLoadsInboxAndClearsAuthenticationError() = runTest {
        provider.result = Result.Error(MessagingAuthenticationRequiredException())
        val viewModel = viewModel(messagesRepository(messages = listOf(message())))
        advanceUntilIdle()
        assertEquals("Sign in to access your messages.", viewModel.uiState.value.errorMessage)
        provider.result = Result.Success(alice)
        viewModel.loadConversations()
        assertTrue(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
        advanceUntilIdle()
        assertEquals(listOf(message()), viewModel.uiState.value.currentMessages)
    }

    @Test
    fun loadingAndFailedSendExposeProgressAndKeepDraftForRetry() = runTest {
        val backing = messagesRepository()
        val repository = object : MessagesRepository by backing {
            override suspend fun sendMessage(message: Message): Result<Message> {
                delay(1000)
                return Result.Error(IllegalStateException("Private transport detail"))
            }
        }
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.onMessageInputChanged("Hello")
        viewModel.sendCurrentMessage()
        runCurrent()
        assertTrue(viewModel.uiState.value.isSending)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isSending)
        assertEquals("Hello", viewModel.uiState.value.messageInput)
        assertNotNull(viewModel.uiState.value.errorMessage)
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

    @Test
    fun selectingConversationExposesMessageLoadingState() = runTest {
        val other = conversation(ConversationId("other"))
        val backing = messagesRepository(listOf(conversation(), other))
        val repository = object : MessagesRepository by backing {
            override suspend fun getMessages(userId: SenderId, conversationId: ConversationId): Result<List<Message>> {
                delay(1000)
                return backing.getMessages(userId, conversationId)
            }
        }
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.selectConversation(other.id)
        assertTrue(viewModel.uiState.value.isLoadingMessages)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoadingMessages)
    }

    @Test
    fun selectingAnotherConversationCancelsThePreviousMessageLoad() = runTest {
        val other = conversation(ConversationId("other"))
        val backing = messagesRepository(
            listOf(conversation(), other),
            listOf(message(), message(inConversation = other.id))
        )
        val repository = object : MessagesRepository by backing {
            override suspend fun getMessages(userId: SenderId, conversationId: ConversationId): Result<List<Message>> {
                if (conversationId != other.id) delay(1000)
                return backing.getMessages(userId, conversationId)
            }
        }
        val viewModel = viewModel(repository)
        runCurrent()
        viewModel.selectConversation(other.id)
        advanceUntilIdle()
        assertEquals(other.id, viewModel.uiState.value.selectedConversationId)
        assertEquals(listOf(message(inConversation = other.id)), viewModel.uiState.value.currentMessages)
    }

    @Test
    fun identityFailureCancelsOutstandingLoadsSoTheyCannotRestorePrivateMessages() = runTest {
        val backing = messagesRepository(messages = listOf(message()))
        val repository = object : MessagesRepository by backing {
            override suspend fun getMessages(userId: SenderId, conversationId: ConversationId): Result<List<Message>> {
                delay(1000)
                return backing.getMessages(userId, conversationId)
            }
        }
        val viewModel = viewModel(repository)
        runCurrent()
        provider.result = Result.Error(MessagingAuthenticationRequiredException())
        viewModel.report(message().id)
        advanceUntilIdle()
        assertEquals(emptyList(), viewModel.uiState.value.currentMessages)
        assertEquals(emptyList(), viewModel.uiState.value.conversations)
    }

    private fun viewModel(repository: MessagesRepository) = MessagesViewModel(
        GetConversations(repository, provider),
        GetConversationMessages(repository, provider),
        SendMessage(repository, provider, nextMessageId = { MessageId("new-message") }, nowMillis = { 123L }),
        ReportMessage(repository, provider)
    )
}
