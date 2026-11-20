package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.ConversationReviews
import com.jdrms.bulletin.domain.messages.application.CurrentMessageSenderProvider
import com.jdrms.bulletin.domain.messages.application.MessageSender
import com.jdrms.bulletin.domain.messages.application.SubmitConversationReview
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationReviewInteractionTest {
    @Test
    fun waitsForSaveAndPreventsDuplicateSubmissions() = runTest {
        val pending = CompletableDeferred<Result<Unit>>()
        var calls = 0
        val submit = useCase { listing, buyer, seller, rating, comment ->
            calls++
            assertEquals("listing:1", listing.value)
            assertEquals("buyer", buyer.value)
            assertEquals("seller", seller.value)
            assertEquals(4, rating)
            assertEquals("Great", comment)
            pending.await()
        }
        val interaction = ConversationReviewInteraction(submit, ::state, this)
        interaction.submit(4, " Great ")
        interaction.submit(4, "Great")
        assertEquals(ConversationReviewState.Submitting, interaction.state.value[SALE_LISTING_ID])
        runCurrent()
        assertEquals(1, calls)
        pending.complete(Result.Success(Unit))
        runCurrent()
        assertEquals(ConversationReviewState.Submitted, interaction.state.value[SALE_LISTING_ID])
        interaction.submit(4, "Great")
        runCurrent()
        assertEquals(1, calls)
    }

    @Test
    fun failedSaveAllowsRetry() = runTest {
        var calls = 0
        val submit = useCase { _, _, _, _, _ ->
            if (++calls == 1) Result.Error(IllegalStateException("Offline")) else Result.Success(Unit)
        }
        val interaction = ConversationReviewInteraction(submit, ::state, this)
        interaction.submit(5, "")
        runCurrent()
        assertEquals(ConversationReviewState.Failed("Offline"), interaction.state.value[SALE_LISTING_ID])
        interaction.submit(5, "")
        runCurrent()
        assertEquals(ConversationReviewState.Submitted, interaction.state.value[SALE_LISTING_ID])
    }

    @Test
    fun accountChangeRejectsSubmissionAndDiscardsOldResults() = runTest {
        var calls = 0
        val differentIdentity = SubmitConversationReview(
            ConversationReviews { _, _, _, _, _ ->
                calls++
                Result.Success(Unit)
            },
            CurrentMessageSenderProvider { Result.Success(MessageSender(SenderId("other"), "Other")) }
        )
        assertIs<Result.Error>(differentIdentity(ListingReferenceId("1"), SenderId("buyer"), SenderId("seller"), 5, ""))
        assertEquals(0, calls)
        val pending = CompletableDeferred<Result<Unit>>()
        val interaction = ConversationReviewInteraction(useCase { _, _, _, _, _ -> pending.await() }, ::state, this)
        interaction.submit(5, "")
        runCurrent()
        interaction.reset()
        pending.complete(Result.Success(Unit))
        runCurrent()
        assertTrue(interaction.state.value.isEmpty())
    }

    @Test
    fun restoredListingCannotSubmit() = runTest {
        val interaction = ConversationReviewInteraction(
            useCase { _, _, _, _, _ -> error("Must not save") },
            { state().copy(listingPreviews = mapOf(SALE_LISTING_ID to listingSalePreview())) },
            this
        )
        interaction.submit(5, "")
        runCurrent()
        assertTrue(interaction.state.value.isEmpty())
    }

    private fun useCase(port: ConversationReviews) = SubmitConversationReview(
        port,
        CurrentMessageSenderProvider { Result.Success(MessageSender(SenderId("buyer"), "Buyer")) }
    )

    private fun state(): MessagesUiState {
        val conversation = Conversation(
            ConversationId("chat"),
            listOf(
                ConversationParticipant(SenderId("buyer"), "Buyer"),
                ConversationParticipant(SenderId("seller"), "Seller")
            ),
            listingId = ListingReferenceId(SALE_LISTING_ID)
        )
        return MessagesUiState(
            viewerId = SenderId("buyer"),
            conversations = listOf(conversation),
            selectedConversationId = conversation.id,
            listingPreviews = mapOf(SALE_LISTING_ID to listingSalePreview(sold = true))
        )
    }
}
