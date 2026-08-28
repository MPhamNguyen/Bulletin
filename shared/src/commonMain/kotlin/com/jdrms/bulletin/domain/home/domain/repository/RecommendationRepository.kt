package com.jdrms.bulletin.domain.home.domain.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.domain.model.Recommendation
import com.jdrms.bulletin.domain.home.domain.model.UserPreferences
import com.jdrms.bulletin.domain.profile.domain.model.UserId

interface RecommendationRepository {
    suspend fun getRecommendations(userId: UserId, preferences: UserPreferences?): List<Recommendation>
    suspend fun updateUserPreferences(userId: UserId, preferences: UserPreferences): Result<Unit>
}
