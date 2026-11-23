package com.jdrms.bulletin.domain.messages.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.ConversationLockReason
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository

data class MessageInbox(
    val viewerId: SenderId,
    val conversations: List<Conversation>,
    val pendingReviewConversationIds: Set<ConversationId>? = emptySet(),
    val unmatchedReviewRequests: List<PendingConversationReview>? = emptyList()
)

class GetConversations(
    private val repository: MessagesRepository,
    private val currentSenderProvider: CurrentMessageSenderProvider,
    private val reviews: PendingConversationReviews = PendingConversationReviews { Result.Success(emptyList()) }
) {
    suspend operator fun invoke(): Result<MessageInbox> {
        val viewer = when (val sender = currentSenderProvider.getCurrentSender()) {
            is Result.Success -> sender.data.id
            is Result.Error -> return sender
        }
        val conversations = when (val result = repository.getConversations(viewer)) {
            is Result.Success -> result.data
            is Result.Error -> return result
        }
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

    private fun Conversation.matchesReview(request: PendingConversationReview, viewer: SenderId): Boolean =
        includes(viewer) && lockReason != ConversationLockReason.ACCOUNT_DELETED &&
            request.listingId.value.removePrefix("listing:") == listingId?.value?.removePrefix("listing:") &&
            counterpartId(viewer) == request.sellerId

    private fun Conversation.counterpartId(viewer: SenderId): SenderId? =
        participants.singleOrNull { it.id != viewer }?.id
}
