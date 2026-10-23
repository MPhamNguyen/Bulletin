package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.GetConversationMessages
import com.jdrms.bulletin.domain.messages.application.HideConversation
import com.jdrms.bulletin.domain.messages.application.MarkConversationRead
import com.jdrms.bulletin.domain.messages.application.ObserveConversationMessages
import com.jdrms.bulletin.domain.messages.application.ReportMessage
import com.jdrms.bulletin.domain.messages.application.SendMessage
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MessagesReadReceiptViewModelTest {
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
    fun openingConversationWithUnreadMessagesMarksItSeen() = runTest {
        val backing = messagesRepository(messages = listOf(message(sender = carol)))
        var markReadCalls = 0
        val repository = object : MessagesRepository by backing {
            override suspend fun markConversationRead(
                userId: SenderId,
                conversationId: ConversationId
            ): Result<kotlin.time.Instant> {
                markReadCalls++
                delay(1_000)
                return backing.markConversationRead(userId, conversationId)
            }
        }
        val viewModel = MessagesViewModel(
            testInboxOwner(repository, provider, dispatcher),
            GetConversationMessages(repository, provider),
            ObserveConversationMessages(backing, provider),
            SendMessage(repository, provider),
            ReportMessage(repository, provider),
            HideConversation(repository, provider),
            markConversationRead = MarkConversationRead(repository, provider)
        ).also { it.onDestinationActiveChanged(true) }
        advanceUntilIdle()

        viewModel.selectConversation(conversationId)
        runCurrent()

        assertTrue(markReadCalls > 0)
        assertNotNull(
            viewModel.inboxState.value.conversations.single()
                .participants.first { it.id == alice.id }.lastReadAt
        )

        advanceUntilIdle()

        assertNotNull(
            repository.getConversations(alice.id).getOrThrow().single()
                .participants.first { it.id == alice.id }.lastReadAt
        )
    }
}
