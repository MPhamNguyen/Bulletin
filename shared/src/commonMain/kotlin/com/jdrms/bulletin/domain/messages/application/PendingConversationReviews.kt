package com.jdrms.bulletin.domain.messages.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId

data class PendingConversationReview(val listingId: ListingReferenceId, val sellerId: SenderId)

/** Buyer-actionable review requests, translated by the owning context. */
fun interface PendingConversationReviews {
    suspend fun forBuyer(buyerId: SenderId): Result<List<PendingConversationReview>>
}
