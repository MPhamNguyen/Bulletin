package com.jdrms.bulletin.domain.messages.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository

data class MessageInbox(
    val viewerId: SenderId,
    val conversations: List<Conversation>,
    val pendingReviewConversationIds: Set<ConversationId>? = emptySet()
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
        val pending = reviews.forBuyer(viewer).getOrNull()?.let { requests ->
            conversations.filter { conversation ->
                conversation.includes(viewer) && !conversation.isReadOnly && requests.any { request ->
                    request.listingId.value.removePrefix("listing:") ==
                        conversation.listingId?.value?.removePrefix("listing:") &&
                        conversation.counterpartId(viewer) == request.sellerId
                }
            }.mapTo(mutableSetOf(), Conversation::id)
        }
        return Result.Success(MessageInbox(viewer, conversations, pending))
    }

    private fun Conversation.counterpartId(viewer: SenderId): SenderId? =
        participants.singleOrNull { it.id != viewer }?.id
}
