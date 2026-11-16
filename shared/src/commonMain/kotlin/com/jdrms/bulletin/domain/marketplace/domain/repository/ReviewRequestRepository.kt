package com.jdrms.bulletin.domain.marketplace.domain.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequest
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestStatus

interface ReviewRequestRepository {
    suspend fun createIfMissing(request: ReviewRequest): Result<Unit>
    suspend fun pendingForBuyer(buyerId: String): List<ReviewRequest>

    /**
     * Applies a buyer-driven status change following `ReviewRequestTransitions`, the only
     * place legal transitions are defined.
     *
     * - `Success`: the change was applied, or the request already has [status] (repeating ACCEPTED, DECLINED or
     *   SUBMITTED is an idempotent no-op, including repeated submission).
     * - `Error(ReviewRequestNotFoundException)`: no such request.
     * - `Error(IllegalReviewRequestTransitionException)`: the change is not in the table, e.g. anything out of a
     *   terminal state or DECLINED after ACCEPTED.
     * - `Error(IllegalArgumentException)`: [status] is not buyer-settable (PENDING or CANCELLED).
     */
    suspend fun updateStatus(listingId: String, buyerId: String, status: ReviewRequestStatus): Result<Unit>

    suspend fun cancelForRestoredListing(listingId: String, sellerId: String): Result<Unit>
}
