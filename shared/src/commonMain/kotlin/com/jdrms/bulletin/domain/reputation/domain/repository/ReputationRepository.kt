package com.jdrms.bulletin.domain.reputation.domain.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.reputation.domain.model.Review
import com.jdrms.bulletin.domain.reputation.domain.model.RevieweeId
import com.jdrms.bulletin.domain.reputation.domain.model.UserReputation

interface ReputationRepository {
    suspend fun getReviewsForUser(userId: RevieweeId): UserReputation
    suspend fun submitReview(review: Review): Result<Review>
}
