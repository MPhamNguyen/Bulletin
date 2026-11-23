package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import com.jdrms.bulletin.domain.messages.infrastructure.repository.SupabaseMessagesRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class InboxAvatarSnapshotTest {
    @Test
    fun bothRepositoriesPreserveParticipantAvatarUrlsInInboxAndTargetedReads() = runTest {
        val chat = Conversation(
            conversationId,
            listOf(
                ConversationParticipant(alice.id, "Viewer"),
                ConversationParticipant(carol.id, "Peer", avatarUrl = "https://example.com/photo.jpg")
            )
        )
        val repositories: List<MessagesRepository> = listOf(
            messagesRepository(listOf(chat)),
            SupabaseMessagesRepository(FakeSupabaseMessagesTable(listOf(chat)))
        )
        for (repository in repositories) {
            val fetched = repository.getConversationPage(alice.id, 0, 10).getOrThrow().single()
            assertEquals("https://example.com/photo.jpg", fetched.participants.last().avatarUrl)
            assertEquals(
                fetched.participants,
                repository.getConversation(alice.id, conversationId).getOrThrow()?.participants
            )
        }
    }
}
