package com.jdrms.bulletin.domain.recommendations.domain.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.identity.domain.model.UserId
import com.jdrms.bulletin.domain.recommendations.domain.model.Recommendation
import com.jdrms.bulletin.domain.recommendations.domain.model.UserPreferences

interface RecommendationRepository {
    suspend fun getRecommendations(userId: UserId, preferences: UserPreferences?): List<Recommendation>
    suspend fun updateUserPreferences(userId: UserId, preferences: UserPreferences): Result<Unit>
}
