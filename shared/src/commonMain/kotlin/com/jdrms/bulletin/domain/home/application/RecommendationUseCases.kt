package com.jdrms.bulletin.domain.home.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.domain.model.Recommendation
import com.jdrms.bulletin.domain.home.domain.model.UserPreferences
import com.jdrms.bulletin.domain.home.domain.repository.RecommendationRepository
import com.jdrms.bulletin.domain.profile.domain.model.UserId

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
