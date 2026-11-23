package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.alice
import com.jdrms.bulletin.domain.messages.bob
import com.jdrms.bulletin.domain.messages.carol
import com.jdrms.bulletin.domain.messages.conversation
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConversationListingThumbnailPrefetchTest {
    @Test
    fun firstPageOnlyIsWarmedAndCropVariantsShareAnImageRequest() {
        val conversations = (0..10).map { index ->
            Conversation(
                ConversationId("chat-$index"),
                conversation().participants,
                listingThumbnailUrl = "https://example.com/$index.jpg"
            )
        }
        assertEquals(
            (0..9).map { "https://example.com/$it.jpg" },
            conversationListingPrefetchUrls(alice.id, conversations)
        )
        val variants = listOf(
            Conversation(
                ConversationId("one"),
                conversation().participants,
                listingThumbnailUrl = "cover.jpg#bulletin_crop=1.0,0.0,0.0"
            ),
            Conversation(
                ConversationId("two"),
                conversation().participants,
                listingThumbnailUrl = "cover.jpg#bulletin_crop=2.0,0.2,0.2"
            ),
            Conversation(
                ConversationId("unrelated"),
                conversation(senders = listOf(bob, carol)).participants,
                listingThumbnailUrl = "private.jpg"
            ),
            Conversation(ConversationId("missing"), conversation().participants)
        )
        assertEquals(listOf("cover.jpg#bulletin_crop=1.0,0.0,0.0"), conversationListingPrefetchUrls(alice.id, variants))
        assertTrue(conversationListingPrefetchUrls(null, variants).isEmpty())
    }
}
