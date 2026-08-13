package com.jdrms.bulletin.domain.reputation.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.reputation.domain.model.Review
import com.jdrms.bulletin.domain.reputation.domain.model.RevieweeId
import com.jdrms.bulletin.domain.reputation.domain.model.UserReputation
import com.jdrms.bulletin.domain.reputation.domain.repository.ReputationRepository
import com.jdrms.bulletin.domain.reputation.domain.service.ReputationCalculationPolicy

class GetStudentReputation(
    private val reputationRepository: ReputationRepository,
    private val reputationPolicy: ReputationCalculationPolicy = ReputationCalculationPolicy()
) {
    suspend operator fun invoke(userId: RevieweeId): UserReputation {
        val reviews = reputationRepository.getReviewsForUser(userId)
        return reputationPolicy.calculateUserReputation(userId, reviews)
    }
}

class SubmitReview(
    private val reputationRepository: ReputationRepository,
    private val reputationPolicy: ReputationCalculationPolicy = ReputationCalculationPolicy()
) {
    suspend operator fun invoke(review: Review): Result<Review> {
        val validation = reputationPolicy.validateNewReview(review)
        if (validation is Result.Error) {
            return validation
        }
        return reputationRepository.submitReview(review)
    }
}
