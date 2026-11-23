package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.ConversationListingPreview
import com.jdrms.bulletin.domain.messages.application.ConversationListingPreviewProvider
import com.jdrms.bulletin.domain.messages.application.GetConversationMessages
import com.jdrms.bulletin.domain.messages.application.GetConversations
import com.jdrms.bulletin.domain.messages.application.HideConversation
import com.jdrms.bulletin.domain.messages.application.ObserveConversationMessages
import com.jdrms.bulletin.domain.messages.application.ObserveInboxInvalidations
import com.jdrms.bulletin.domain.messages.application.PendingConversationReview
import com.jdrms.bulletin.domain.messages.application.PendingConversationReviews
import com.jdrms.bulletin.domain.messages.application.ReportMessage
import com.jdrms.bulletin.domain.messages.application.SendMessage
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import com.jdrms.bulletin.domain.messages.domain.repository.InboxObserver
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesObserver
import com.jdrms.bulletin.domain.messages.presentation.InboxStateOwner
import com.jdrms.bulletin.domain.messages.presentation.MessagesViewModel
import com.jdrms.bulletin.domain.messages.presentation.buyerRatingPromptSeller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MessagesReviewRefreshTest {
    private val dispatcher = StandardTestDispatcher()
    private val listing = ListingReferenceId("1")
    private val chat = Conversation(conversationId, conversation().participants, listingId = listing)
    private val repository = messagesRepository(listOf(chat))
    private val identity = MutableMessageSenderProvider()
    private var pending = emptyList<PendingConversationReview>()
    private var sold = false

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun chatEntryAndReentryRefreshReviewRequestsWithoutInboxEvents() = runTest(dispatcher) {
        val inbox = InboxStateOwner(
            GetConversations(repository, identity, PendingConversationReviews { Result.Success(pending) }),
            ObserveInboxInvalidations(InboxObserver { emptyFlow() }),
            identity,
            backgroundScope
        )
        val viewModel = viewModel(inbox)
        viewModel.onDestinationActiveChanged(true)
        runCurrent()
        assertTrue(viewModel.uiState.value.pendingReviewConversationIds.isEmpty())

        sold = true
        pending = listOf(PendingConversationReview(listing, carol.id))
        viewModel.selectConversation(chat.id)
        runCurrent()
        val state = viewModel.uiState.value
        assertEquals(setOf(chat.id), state.pendingReviewConversationIds)
        assertNotNull(
            buyerRatingPromptSeller(state.listingPreviews[listing.value], state.viewerId, chat.participants.last())
        )

        viewModel.returnToInbox()
        pending = emptyList()
        viewModel.selectConversation(chat.id)
        runCurrent()
        assertTrue(viewModel.uiState.value.pendingReviewConversationIds.isEmpty())

        viewModel.returnToInbox()
        pending = listOf(PendingConversationReview(listing, carol.id))
        viewModel.selectConversation(chat.id)
        runCurrent()
        assertEquals(setOf(chat.id), viewModel.uiState.value.pendingReviewConversationIds)
        inbox.close()
    }

    private fun viewModel(inbox: InboxStateOwner) = MessagesViewModel(
        inbox,
        GetConversationMessages(repository, identity),
        ObserveConversationMessages(MessagesObserver { _, _ -> emptyFlow() }, identity),
        SendMessage(repository, identity),
        ReportMessage(repository, identity),
        HideConversation(repository, identity),
        ConversationListingPreviewProvider {
            ConversationListingPreview(
                "Book",
                "$10",
                null,
                sellerId = carol.id.value,
                isSold = sold,
                soldToBuyerId = if (sold) alice.id.value else null
            )
        }
    )
}
