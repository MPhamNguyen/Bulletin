package com.jdrms.bulletin.domain.recommendations.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.network.SupabaseConfig
import com.jdrms.bulletin.domain.identity.domain.model.UserId
import com.jdrms.bulletin.domain.marketplace.domain.repository.ListingRepository
import com.jdrms.bulletin.domain.recommendations.domain.model.Recommendation
import com.jdrms.bulletin.domain.recommendations.domain.model.UserPreferences
import com.jdrms.bulletin.domain.recommendations.domain.repository.RecommendationRepository
import com.jdrms.bulletin.domain.recommendations.domain.service.RecommendationRankingService

class SupabaseRecommendationRepository(
    private val listingRepository: ListingRepository,
    private val rankingService: RecommendationRankingService = RecommendationRankingService(),
    private val supabaseConfig: SupabaseConfig = SupabaseConfig()
) : RecommendationRepository {

    private val userPrefsMap = mutableMapOf<String, UserPreferences>()

    override suspend fun getRecommendations(userId: UserId, preferences: UserPreferences?): List<Recommendation> {
        val userPrefs = preferences ?: userPrefsMap[userId.value] ?: UserPreferences()
        val allListings = listingRepository.getListings()
        return rankingService.rankListings(allListings, userPrefs)
    }

    override suspend fun updateUserPreferences(userId: UserId, preferences: UserPreferences): Result<Unit> {
        userPrefsMap[userId.value] = preferences
        return Result.Success(Unit)
    }
}
