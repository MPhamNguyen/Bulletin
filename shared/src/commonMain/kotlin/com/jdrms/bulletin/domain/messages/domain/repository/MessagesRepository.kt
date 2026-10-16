package com.jdrms.bulletin.domain.messages.domain.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import com.jdrms.bulletin.domain.messages.domain.model.MESSAGE_PAGE_SIZE
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageCursor
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.MessagePage
import com.jdrms.bulletin.domain.messages.domain.model.SenderId

/**
 * Every operation is scoped to a participant. Missing and inaccessible conversations return the same error.
 * Adapters must enforce membership for reads and writes; names are display metadata, never identity.
 * A network adapter must also enforce these rules on the server using the authenticated session.
 * Message lists are ordered oldest to newest by timestamp, then by ID for equal timestamps.
 */
interface MessagesRepository {
    suspend fun getOrCreateConversation(
        requesterId: SenderId,
        otherParticipant: ConversationParticipant,
        listingId: ListingReferenceId
    ): Result<Conversation>
    suspend fun getConversations(userId: SenderId): Result<List<Conversation>>
    suspend fun hideConversation(userId: SenderId, conversationId: ConversationId): Result<Unit>
    suspend fun getMessagePage(
        userId: SenderId,
        conversationId: ConversationId,
        before: MessageCursor? = null,
        limit: Int = MESSAGE_PAGE_SIZE
    ): Result<MessagePage>
    suspend fun sendMessage(message: Message): Result<Message>
    suspend fun reportMessage(
        userId: SenderId,
        conversationId: ConversationId,
        messageId: MessageId,
        reason: String
    ): Result<Unit>
}
