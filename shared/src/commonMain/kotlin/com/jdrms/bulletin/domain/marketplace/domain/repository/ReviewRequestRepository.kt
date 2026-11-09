package com.jdrms.bulletin.domain.marketplace.domain.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequest
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestStatus

interface ReviewRequestRepository {
    suspend fun createIfMissing(request: ReviewRequest): Result<Unit>
    suspend fun pendingForBuyer(buyerId: String): List<ReviewRequest>
    suspend fun updateStatus(listingId: String, buyerId: String, status: ReviewRequestStatus): Result<Unit>
    suspend fun cancelForRestoredListing(listingId: String, sellerId: String): Result<Unit>
}
