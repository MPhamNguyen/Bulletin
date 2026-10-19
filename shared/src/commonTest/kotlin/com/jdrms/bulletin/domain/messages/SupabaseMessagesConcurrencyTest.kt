package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationMembershipDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationParticipantDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageDto
import com.jdrms.bulletin.domain.messages.infrastructure.repository.SupabaseMessagesRepository
import com.jdrms.bulletin.domain.messages.infrastructure.repository.SupabaseMessagesTable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class SupabaseMessagesConcurrencyTest {
    @Test
    fun inboxPreservesConversationOrdering() = runTest {
        val conversations = (1..7).map { index ->
            val source = conversation(ConversationId("conversation-$index"))
            Conversation(source.id, source.participants, updatedAtMillis = index.toLong())
        }
        val table = FakeSupabaseMessagesTable(conversations)
        val repository = SupabaseMessagesRepository(table)

        val result = repository.getConversations(alice.id)

        assertEquals(
            conversations.sortedByDescending(Conversation::updatedAtMillis).map(Conversation::id),
            assertIs<Result.Success<List<Conversation>>>(result).data.map(Conversation::id)
        )
    }

    @Test
    fun openingConversationWaitsForIndependentReads() = runTest {
        val listingId = ListingReferenceId("listing-a")
        val source = conversation()
        val backing = FakeSupabaseMessagesTable(
            listOf(Conversation(source.id, source.participants, listingId = listingId)),
            listOf(message())
        )
        val table = object : SupabaseMessagesTable by backing {
            override suspend fun findConversation(conversationId: String): SupabaseConversationDto? {
                delay(100)
                return backing.findConversation(conversationId)
            }
            override suspend fun getParticipants(conversationId: String): List<SupabaseConversationParticipantDto> {
                delay(100)
                return backing.getParticipants(conversationId)
            }
            override suspend fun findLatestMessage(conversationId: String): SupabaseMessageDto? {
                delay(100)
                return backing.findLatestMessage(conversationId)
            }
            override suspend fun getMembership(
                conversationId: String,
                userId: String
            ): SupabaseConversationMembershipDto? {
                delay(100)
                return backing.getMembership(conversationId, userId)
            }
        }
        val startedAt = testScheduler.currentTime

        val result = SupabaseMessagesRepository(table).getOrCreateConversation(
            alice.id,
            ConversationParticipant(carol.id, carol.displayName),
            listingId
        )

        assertIs<Result.Success<Conversation>>(result)
        assertEquals(100L, testScheduler.currentTime - startedAt)
    }
}
