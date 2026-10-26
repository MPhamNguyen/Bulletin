package com.jdrms.bulletin.domain.messages.domain.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import com.jdrms.bulletin.domain.messages.domain.model.MESSAGE_PAGE_SIZE
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageCursor
import com.jdrms.bulletin.domain.messages.domain.model.MessageEdit
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.MessagePage
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import kotlin.time.Instant

/**
 * Every operation is scoped to a participant. Missing and inaccessible conversations return the same error.
 * Adapters must enforce membership for reads and writes; names are display metadata, never identity.
 * A network adapter must also enforce these rules on the server using the authenticated session.
 * Message lists are ordered oldest to newest by timestamp, then by ID for equal timestamps.
 * A participant who removed a conversation can only read messages after their latest removal time,
 * including after a new message or explicit reopening makes the conversation visible again.
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
    suspend fun markConversationRead(userId: SenderId, conversationId: ConversationId): Result<Instant>
    suspend fun sendMessage(message: Message): Result<Message>

    /** Preserve creation/order/read state; reject stale versions and edits outside the original 15-minute window. */
    suspend fun editMessage(userId: SenderId, edit: MessageEdit): Result<Message>
    suspend fun reportMessage(
        userId: SenderId,
        conversationId: ConversationId,
        messageId: MessageId,
        reason: String,
        description: String = ""
    ): Result<Unit>
}
