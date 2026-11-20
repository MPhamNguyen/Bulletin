package com.jdrms.bulletin.domain.messages.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId

/** Saves a buyer's review through the owning review context. */
fun interface ConversationReviews {
    suspend fun submit(
        listingId: ListingReferenceId,
        buyerId: SenderId,
        sellerId: SenderId,
        rating: Int,
        comment: String
    ): Result<Unit>
}
