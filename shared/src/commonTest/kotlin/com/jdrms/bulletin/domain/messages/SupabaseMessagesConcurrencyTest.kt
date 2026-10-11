package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.MessagesInstrumentation
import com.jdrms.bulletin.domain.messages.application.MessagesTimingEvent
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationParticipantDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageDto
import com.jdrms.bulletin.domain.messages.infrastructure.repository.SupabaseMessagesRepository
import com.jdrms.bulletin.domain.messages.infrastructure.repository.SupabaseMessagesTable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SupabaseMessagesConcurrencyTest {
    @Test
    fun inboxLoadsThreeReadsForBoundedConversationsAndPreservesSortAndCallCount() = runTest {
        val conversations = (1..7).map { index ->
            val source = conversation(ConversationId("conversation-$index"))
            Conversation(source.id, source.participants, updatedAtMillis = index.toLong())
        }
        val table = ConcurrentReadTable(FakeSupabaseMessagesTable(conversations))
        val events = mutableListOf<MessagesTimingEvent>()
        val repository = SupabaseMessagesRepository(
            table,
            maxConcurrentConversations = 2,
            instrumentation = MessagesInstrumentation(events::add)
        )
        val result = async { repository.getConversations(alice.id) }
        runCurrent()

        assertEquals(6, table.peakReadCount)
        assertEquals(2, table.peakActiveConversationCount)
        table.release.complete(Unit)
        assertEquals(
            conversations.sortedByDescending(Conversation::updatedAtMillis).map(Conversation::id),
            result.await().getOrThrow().map(Conversation::id)
        )
        assertEquals(7, table.finishedConversations.size)
        assertEquals(22, events.single().callCount)
        assertEquals(7, events.single().resultCount)
    }

    @Test
    fun inboxFailureCancelsSiblingConversationReads() = runTest {
        val siblingCancelled = CompletableDeferred<Unit>()
        val delegate = FakeSupabaseMessagesTable(listOf(conversation(), conversation(ConversationId("other"))))
        val table = object : SupabaseMessagesTable by delegate {
            override suspend fun findConversation(conversationId: String): SupabaseConversationDto? {
                if (conversationId == com.jdrms.bulletin.domain.messages.conversationId.value) {
                    error("expected storage failure")
                }
                try {
                    awaitCancellation()
                } finally {
                    siblingCancelled.complete(Unit)
                }
            }
        }

        val result = SupabaseMessagesRepository(table, maxConcurrentConversations = 2)
            .getConversations(alice.id)

        assertIs<Result.Error>(result)
        assertTrue(siblingCancelled.isCompleted)
    }

    @Test
    fun parentCancellationCancelsInboxChildren() = runTest {
        val childCancelled = CompletableDeferred<Unit>()
        val table = object : SupabaseMessagesTable by FakeSupabaseMessagesTable(listOf(conversation())) {
            override suspend fun findConversation(conversationId: String): SupabaseConversationDto? {
                try {
                    awaitCancellation()
                } finally {
                    childCancelled.complete(Unit)
                }
            }
        }
        val job: Job = launch { SupabaseMessagesRepository(table).getConversations(alice.id) }
        runCurrent()
        job.cancelAndJoin()

        assertTrue(childCancelled.isCompleted)
    }

    @Test
    fun concurrencyLimitMustBeBetweenOneAndEight() {
        val table = FakeSupabaseMessagesTable(emptyList())
        assertFailsWith<IllegalArgumentException> { SupabaseMessagesRepository(table, 0) }
        assertFailsWith<IllegalArgumentException> { SupabaseMessagesRepository(table, 9) }
    }

    @Test
    fun emptyInboxReturnsEmptyList() = runTest {
        assertEquals(
            emptyList(),
            SupabaseMessagesRepository(FakeSupabaseMessagesTable(emptyList())).getConversations(alice.id).getOrThrow()
        )
    }
}

private class ConcurrentReadTable(
    private val delegate: SupabaseMessagesTable
) : SupabaseMessagesTable by delegate {
    val release = CompletableDeferred<Unit>()
    private val activeConversationIds = mutableSetOf<String>()
    private var activeReadCount = 0
    var peakReadCount = 0
        private set
    var peakActiveConversationCount = 0
        private set
    val finishedConversations = mutableSetOf<String>()

    override suspend fun findConversation(conversationId: String): SupabaseConversationDto? =
        holdConversation(conversationId) { delegate.findConversation(conversationId) }

    override suspend fun getParticipants(conversationId: String): List<SupabaseConversationParticipantDto> =
        holdConversation(conversationId) { delegate.getParticipants(conversationId) }

    override suspend fun findLatestMessage(conversationId: String): SupabaseMessageDto? =
        holdConversation(conversationId) { delegate.findLatestMessage(conversationId) }

    private suspend fun <T> holdConversation(conversationId: String, read: suspend () -> T): T {
        activeReadCount += 1
        activeConversationIds += conversationId
        peakReadCount = maxOf(peakReadCount, activeReadCount)
        peakActiveConversationCount = maxOf(peakActiveConversationCount, activeConversationIds.size)
        try {
            release.await()
            return read()
        } finally {
            activeReadCount -= 1
            activeConversationIds -= conversationId
            if (conversationId !in activeConversationIds) finishedConversations += conversationId
        }
    }
}
