package com.jdrms.bulletin.domain.create_listings.domain.service

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.create_listings.domain.model.Review
import com.jdrms.bulletin.domain.create_listings.domain.model.RevieweeId
import com.jdrms.bulletin.domain.create_listings.domain.model.UserReputation

class ReputationCalculationPolicy {
    fun calculateUserReputation(userId: RevieweeId, reviews: List<Review>): UserReputation {
        if (reviews.isEmpty()) {
            return UserReputation(userId = userId, averageRating = 0.0, totalReviews = 0, reviews = emptyList())
        }
        val sum = reviews.sumOf { it.rating.score }
        val avg = (sum.toDouble() / reviews.size * 10).toInt() / 10.0
        return UserReputation(
            userId = userId,
            averageRating = avg,
            totalReviews = reviews.size,
            reviews = reviews
        )
    }

    fun validateNewReview(review: Review): Result<Unit> {
        if (review.reviewerId.value == review.revieweeId.value) {
            return Result.Error(IllegalArgumentException("Users cannot review themselves."))
        }
        if (review.comment.isBlank()) {
            return Result.Error(IllegalArgumentException("Review comment cannot be blank."))
        }
        return Result.Success(Unit)
    }
}
