package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequest
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestStatus
import com.jdrms.bulletin.domain.marketplace.domain.repository.ReviewRequestRepository

class InMemoryReviewRequestRepository : ReviewRequestRepository {
    private val requests = mutableMapOf<Pair<String, String>, ReviewRequest>()

    override suspend fun createIfMissing(request: ReviewRequest): Result<Unit> {
        val key = request.listingId.value to request.buyerId
        val existing = requests[key]
        if (existing == null || existing.status == ReviewRequestStatus.CANCELLED) {
            requests[key] = request
        }
        return Result.Success(Unit)
    }

    override suspend fun pendingForBuyer(buyerId: String): List<ReviewRequest> =
        requests.values.filter {
            it.buyerId == buyerId &&
                (it.status == ReviewRequestStatus.PENDING || it.status == ReviewRequestStatus.ACCEPTED)
        }

    override suspend fun updateStatus(
        listingId: String,
        buyerId: String,
        status: ReviewRequestStatus
    ): Result<Unit> {
        val key = listingId to buyerId
        val current = requests[key] ?: return Result.Error(NoSuchElementException("Review request not found."))
        return runCatching {
            requests[key] = when (status) {
                ReviewRequestStatus.ACCEPTED -> current.accept()
                ReviewRequestStatus.DECLINED -> current.decline()
                ReviewRequestStatus.SUBMITTED -> current.submitted()
                ReviewRequestStatus.CANCELLED -> error("Only the seller can cancel a review request.")
                ReviewRequestStatus.PENDING -> error("A request cannot return to pending.")
            }
        }.fold({ Result.Success(Unit) }, { Result.Error(it) })
    }

    override suspend fun cancelForRestoredListing(listingId: String, sellerId: String): Result<Unit> {
        val id = listingId.removePrefix("listing:")
        val matching = requests.filterKeys { it.first == id }.values
        if (matching.any { it.sellerId != sellerId }) {
            return Result.Error(IllegalStateException("Only the seller can cancel these review requests."))
        }
        matching.filter {
            it.status == ReviewRequestStatus.PENDING || it.status == ReviewRequestStatus.ACCEPTED
        }.forEach { request ->
            requests[id to request.buyerId] = request.cancel()
        }
        return Result.Success(Unit)
    }
}
