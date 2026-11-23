package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.repository.ReviewRequestRepository
import com.jdrms.bulletin.domain.messages.application.PendingConversationReview
import com.jdrms.bulletin.domain.messages.application.PendingConversationReviews
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import kotlinx.coroutines.CancellationException

internal class MarketplacePendingConversationReviews(
    private val requests: ReviewRequestRepository
) : PendingConversationReviews {
    override suspend fun forBuyer(buyerId: SenderId): Result<List<PendingConversationReview>> = runCatching {
        requests.pendingForBuyer(buyerId.value).filter {
            it.buyerId == buyerId.value && it.isActionableByBuyer
        }.map { PendingConversationReview(ListingReferenceId(it.listingId.value), SenderId(it.sellerId)) }
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = {
            if (it is CancellationException) throw it
            Result.Error(it)
        }
    )
}
