package com.jdrms.bulletin.domain.recommendations.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.identity.domain.model.UserId
import com.jdrms.bulletin.domain.recommendations.domain.model.Recommendation
import com.jdrms.bulletin.domain.recommendations.domain.model.UserPreferences
import com.jdrms.bulletin.domain.recommendations.domain.repository.RecommendationRepository

class GetPersonalizedFeed(
    private val recommendationRepository: RecommendationRepository
) {
    suspend operator fun invoke(userId: UserId, preferences: UserPreferences? = null): List<Recommendation> {
        return recommendationRepository.getRecommendations(userId, preferences)
    }
}

class UpdateUserPreferences(
    private val recommendationRepository: RecommendationRepository
) {
    suspend operator fun invoke(userId: UserId, preferences: UserPreferences): Result<Unit> {
        return recommendationRepository.updateUserPreferences(userId, preferences)
    }
}
