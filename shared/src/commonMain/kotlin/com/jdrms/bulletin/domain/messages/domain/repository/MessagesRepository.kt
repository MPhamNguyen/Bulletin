package com.jdrms.bulletin.domain.messages.domain.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId

/**
 * Every operation is scoped to a participant. Missing and inaccessible conversations return the same error.
 * Adapters must enforce membership for reads and writes; names are display metadata, never identity.
 * A network adapter must also enforce these rules on the server using the authenticated session.
 */
interface MessagesRepository {
    suspend fun getOrCreateConversation(
        requesterId: SenderId,
        otherParticipant: ConversationParticipant,
        listingId: ListingReferenceId
    ): Result<Conversation>
    suspend fun getConversations(userId: SenderId): Result<List<Conversation>>
    suspend fun getMessages(userId: SenderId, conversationId: ConversationId): Result<List<Message>>
    suspend fun sendMessage(message: Message): Result<Message>
    suspend fun reportMessage(
        userId: SenderId,
        conversationId: ConversationId,
        messageId: MessageId,
        reason: String
    ): Result<Unit>
}
