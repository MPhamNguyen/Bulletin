package com.jdrms.bulletin.domain.messages.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId

/** Messages-owned port for the seller changing the sale state of the listing a conversation is about. */
interface ConversationListingSales {
    suspend fun markSold(listingId: ListingReferenceId, buyerId: String): Result<Unit>

    suspend fun undoSale(listingId: ListingReferenceId): Result<Unit>
}
