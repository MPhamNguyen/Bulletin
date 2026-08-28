package com.jdrms.bulletin.domain.create_listings.domain.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.create_listings.domain.model.Review
import com.jdrms.bulletin.domain.create_listings.domain.model.RevieweeId

interface ReputationRepository {
    suspend fun getReviewsForUser(userId: RevieweeId): List<Review>
    suspend fun submitReview(review: Review): Result<Review>
}
