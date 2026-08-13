package com.jdrms.bulletin.domain.reputation.domain.model

import com.jdrms.bulletin.core.common.currentTimeMillis
import kotlin.jvm.JvmInline

@JvmInline
value class ReviewId(val value: String)

@JvmInline
value class ReviewerId(val value: String)

@JvmInline
value class RevieweeId(val value: String)

data class Rating(val score: Int) {
    init {
        require(score in 1..5) { "Rating score must be between 1 and 5 stars." }
    }
}

data class Review(
    val id: ReviewId,
    val reviewerId: ReviewerId,
    val reviewerName: String,
    val revieweeId: RevieweeId,
    val rating: Rating,
    val comment: String,
    val createdAtMillis: Long = currentTimeMillis()
)

data class UserReputation(
    val userId: RevieweeId,
    val averageRating: Double,
    val totalReviews: Int,
    val reviews: List<Review>
)
