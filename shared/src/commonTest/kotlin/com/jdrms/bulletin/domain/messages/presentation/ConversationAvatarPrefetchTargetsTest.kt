package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.alice
import com.jdrms.bulletin.domain.messages.bob
import com.jdrms.bulletin.domain.messages.carol
import com.jdrms.bulletin.domain.messages.conversation
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConversationAvatarPrefetchTargetsTest {
    @Test
    fun onlyTheFirstTenConversationAvatarsArePrefetched() {
        val conversations = (0..10).map { index ->
            Conversation(
                ConversationId("chat-$index"),
                listOf(
                    ConversationParticipant(alice.id, "Viewer"),
                    ConversationParticipant(SenderId("peer-$index"), "Peer")
                )
            )
        }
        assertEquals(
            (0..9).map { SenderId("peer-$it") },
            conversationAvatarPrefetchTargets(alice.id, conversations).map { it.senderId }
        )
    }

    @Test
    fun repeatedPeersAreDeduplicatedAndUnavailableAvatarsAreSkipped() {
        val conversations = listOf(
            conversation(),
            conversation(ConversationId("same-peer")),
            conversation(ConversationId("not-a-member"), listOf(bob, carol)),
            conversation(ConversationId("deleted-peer"), listOf(alice)),
            conversation(ConversationId("other-peer"), listOf(alice, bob))
        )
        assertEquals(
            listOf(carol.id, bob.id),
            conversationAvatarPrefetchTargets(alice.id, conversations).map { it.senderId }
        )
        assertTrue(conversationAvatarPrefetchTargets(null, conversations).isEmpty())
    }
}
