package com.jdrms.bulletin.domain.messages.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId

class SubmitConversationReview(
    private val reviews: ConversationReviews,
    private val identity: CurrentMessageSenderProvider
) {
    suspend operator fun invoke(
        listingId: ListingReferenceId,
        buyerId: SenderId,
        sellerId: SenderId,
        rating: Int,
        comment: String
    ): Result<Unit> {
        val sender = when (val result = identity.getCurrentSender()) {
            is Result.Success -> result.data
            is Result.Error -> return result
        }
        if (sender.id != buyerId) return Result.Error(MessagingAuthenticationRequiredException())
        return reviews.submit(listingId, sender.id, sellerId, rating, comment.trim())
    }
}
