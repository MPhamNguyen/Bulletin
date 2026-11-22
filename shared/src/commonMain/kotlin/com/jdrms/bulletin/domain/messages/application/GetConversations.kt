package com.jdrms.bulletin.domain.messages.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository

class GetConversations(
    private val repository: MessagesRepository,
    private val currentSenderProvider: CurrentMessageSenderProvider,
    private val reviews: PendingConversationReviews = PendingConversationReviews { Result.Success(emptyList()) }
) {
    suspend operator fun invoke(
        offset: Int? = null,
        includeReviews: Boolean = true,
        limit: Int = PAGE_SIZE
    ): Result<MessageInbox> {
        val viewer = when (val sender = currentSenderProvider.getCurrentSender()) {
            is Result.Success -> sender.data.id
            is Result.Error -> return sender
        }
        val fetched = if (offset == null) {
            repository.getConversations(viewer)
        } else {
            repository.getConversationPage(viewer, offset, limit)
        }
        val conversations = when (val result = fetched) {
            is Result.Success -> result.data
            is Result.Error -> return result
        }
        val inbox = MessageInbox(viewer, conversations, null, null)
        return if (includeReviews) enrich(inbox) else Result.Success(inbox)
    }

    suspend fun enrich(inbox: MessageInbox): Result<MessageInbox> {
        val viewer = inbox.viewerId
        val conversations = inbox.conversations
        val requests = reviews.forBuyer(viewer).getOrNull()
        val pending = requests?.let { requests ->
            conversations.filter { conversation ->
                requests.any { request -> conversation.matchesReview(request, viewer) }
            }.mapTo(mutableSetOf(), Conversation::id)
        }
        val unmatched = requests?.filter { request ->
            conversations.none { it.id in pending.orEmpty() && it.matchesReview(request, viewer) }
        }
        return Result.Success(MessageInbox(viewer, conversations, pending, unmatched))
    }

    suspend fun conversation(userId: SenderId, id: ConversationId): Result<Conversation?> =
        repository.getConversation(userId, id)

    private fun Conversation.matchesReview(request: PendingConversationReview, viewer: SenderId): Boolean =
        includes(viewer) && !isReadOnly &&
            request.listingId.value.removePrefix("listing:") == listingId?.value?.removePrefix("listing:") &&
            counterpartId(viewer) == request.sellerId

    private fun Conversation.counterpartId(viewer: SenderId): SenderId? =
        participants.singleOrNull { it.id != viewer }?.id

    companion object {
        /** Rows per inbox page; paging, refresh sizing and first-page media warming all use this. */
        const val PAGE_SIZE = 10
    }
}
