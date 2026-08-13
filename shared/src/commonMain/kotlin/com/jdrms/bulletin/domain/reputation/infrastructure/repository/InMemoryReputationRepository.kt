package com.jdrms.bulletin.domain.reputation.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.reputation.domain.model.Review
import com.jdrms.bulletin.domain.reputation.domain.model.RevieweeId
import com.jdrms.bulletin.domain.reputation.domain.repository.ReputationRepository
import com.jdrms.bulletin.domain.reputation.infrastructure.dto.ReviewDto
import com.jdrms.bulletin.domain.reputation.infrastructure.mapper.ReputationMapper

class InMemoryReputationRepository(
    initialReviews: Map<String, List<ReviewDto>> = defaultSeedReviews()
) : ReputationRepository {

    private val reviewsStorage = initialReviews.mapValues { it.value.toMutableList() }.toMutableMap()

    override suspend fun getReviewsForUser(userId: RevieweeId): List<Review> {
        val dtoList = reviewsStorage[userId.value] ?: emptyList()
        return dtoList.map { ReputationMapper.toDomain(it) }
    }

    override suspend fun submitReview(review: Review): Result<Review> {
        if (review.comment.isBlank()) {
            return Result.Error(IllegalArgumentException("Review comment cannot be blank."))
        }
        if (review.reviewerId.value == review.revieweeId.value) {
            return Result.Error(IllegalArgumentException("Users cannot review themselves."))
        }
        val dto = ReputationMapper.toDto(review)
        val list = reviewsStorage.getOrPut(review.revieweeId.value) { mutableListOf() }
        list.add(dto)
        return Result.Success(review)
    }

    companion object {
        fun defaultSeedReviews(): Map<String, List<ReviewDto>> = mapOf(
            "user_101" to listOf(
                ReviewDto(
                    id = "rev_1",
                    reviewerId = "user_102",
                    reviewerName = "Sean Gallagher",
                    revieweeId = "user_101",
                    score = 5,
                    comment = "Great seller! Textbook was in perfect condition and met up right on campus.",
                    createdAtMillis = 1723505000000L
                ),
                ReviewDto(
                    id = "rev_2",
                    reviewerId = "user_103",
                    reviewerName = "Minh Pham-Nguyen",
                    revieweeId = "user_101",
                    score = 5,
                    comment = "Smooth transaction and super responsive messaging.",
                    createdAtMillis = 1723508000000L
                )
            )
        )
    }
}
