package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.IllegalReviewRequestTransitionException
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequest
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestNotFoundException
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestStatus
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestTransition
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestTransitions
import com.jdrms.bulletin.domain.marketplace.domain.repository.ReviewRequestRepository

class InMemoryReviewRequestRepository : ReviewRequestRepository {
    private val requests = mutableMapOf<Pair<String, String>, ReviewRequest>()

    override suspend fun createIfMissing(request: ReviewRequest): Result<Unit> {
        val key = canonical(request.listingId.value) to request.buyerId
        val existing = requests[key]
        if (existing == null || existing.status == ReviewRequestStatus.CANCELLED) {
            requests[key] = request
        }
        return Result.Success(Unit)
    }

    override suspend fun pendingForBuyer(buyerId: String): List<ReviewRequest> =
        requests.values.filter { it.buyerId == buyerId && it.isActionableByBuyer }

    override suspend fun updateStatus(
        listingId: String,
        buyerId: String,
        status: ReviewRequestStatus
    ): Result<Unit> {
        if (status !in ReviewRequestTransitions.buyerSettable) {
            return Result.Error(IllegalArgumentException("Buyers cannot set this review request status."))
        }
        val key = canonical(listingId) to buyerId
        val current = requests[key] ?: return Result.Error(ReviewRequestNotFoundException())
        return when (ReviewRequestTransitions.decide(current.status, status)) {
            ReviewRequestTransition.Apply -> {
                requests[key] = current.copy(status = status)
                Result.Success(Unit)
            }
            ReviewRequestTransition.AlreadyApplied -> Result.Success(Unit)
            is ReviewRequestTransition.Rejected ->
                Result.Error(IllegalReviewRequestTransitionException(current.status, status))
        }
    }

    override suspend fun cancelForRestoredListing(listingId: String, sellerId: String): Result<Unit> {
        val id = canonical(listingId)
        val matching = requests.filterKeys { it.first == id }.values
        if (matching.any { it.sellerId != sellerId }) {
            return Result.Error(IllegalStateException("Only the seller can cancel these review requests."))
        }
        matching.filter { it.canTransitionTo(ReviewRequestStatus.CANCELLED) }.forEach { request ->
            requests[id to request.buyerId] = request.cancel()
        }
        return Result.Success(Unit)
    }

    private fun canonical(listingId: String) = listingId.removePrefix("listing:")
}
