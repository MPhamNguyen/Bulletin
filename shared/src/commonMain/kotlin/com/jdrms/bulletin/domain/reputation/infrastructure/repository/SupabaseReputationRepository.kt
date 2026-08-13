package com.jdrms.bulletin.domain.reputation.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.network.SupabaseConfig
import com.jdrms.bulletin.domain.reputation.domain.model.*
import com.jdrms.bulletin.domain.reputation.domain.repository.ReputationRepository
import com.jdrms.bulletin.domain.reputation.domain.service.ReputationCalculationPolicy
import com.jdrms.bulletin.domain.reputation.infrastructure.dto.ReviewDto
import com.jdrms.bulletin.domain.reputation.infrastructure.mapper.ReputationMapper

class SupabaseReputationRepository(
    private val supabaseConfig: SupabaseConfig = SupabaseConfig(),
    private val calculationPolicy: ReputationCalculationPolicy = ReputationCalculationPolicy()
) : ReputationRepository {

    private val reviewsStorage = mutableMapOf<String, MutableList<ReviewDto>>(
        "user_101" to mutableListOf(
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

    override suspend fun getReviewsForUser(userId: RevieweeId): UserReputation {
        val dtoList = reviewsStorage[userId.value] ?: emptyList()
        val domainList = dtoList.map { ReputationMapper.toDomain(it) }
        return calculationPolicy.calculateUserReputation(userId, domainList)
    }

    override suspend fun submitReview(review: Review): Result<Review> {
        val dto = ReputationMapper.toDto(review)
        val list = reviewsStorage.getOrPut(review.revieweeId.value) { mutableListOf() }
        list.add(dto)
        return Result.Success(review)
    }
}
