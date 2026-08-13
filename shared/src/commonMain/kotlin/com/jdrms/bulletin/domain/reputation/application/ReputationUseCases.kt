package com.jdrms.bulletin.domain.reputation.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.reputation.domain.model.Review
import com.jdrms.bulletin.domain.reputation.domain.model.RevieweeId
import com.jdrms.bulletin.domain.reputation.domain.model.UserReputation
import com.jdrms.bulletin.domain.reputation.domain.repository.ReputationRepository
import com.jdrms.bulletin.domain.reputation.domain.service.ReputationCalculationPolicy

class GetStudentReputation(
    private val reputationRepository: ReputationRepository
) {
    suspend operator fun invoke(userId: RevieweeId): UserReputation {
        return reputationRepository.getReviewsForUser(userId)
    }
}

class SubmitReview(
    private val reputationRepository: ReputationRepository,
    private val reputationPolicy: ReputationCalculationPolicy = ReputationCalculationPolicy()
) {
    suspend operator fun invoke(review: Review): Result<Review> {
        val validation = reputationPolicy.validateNewReview(review)
        if (validation.isError()) {
            return Result.Error((validation as Result.Error).exception)
        }
        return reputationRepository.submitReview(review)
    }
}
