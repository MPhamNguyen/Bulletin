package com.jdrms.bulletin.domain.home.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedItem
import com.jdrms.bulletin.domain.home.domain.model.UserPreferences
import com.jdrms.bulletin.domain.home.domain.repository.HomeRepository
import com.jdrms.bulletin.domain.home.domain.service.HomeRankingPolicy
import com.jdrms.bulletin.domain.home.infrastructure.dto.HomeFeedItemDto
import com.jdrms.bulletin.domain.home.infrastructure.mapper.HomeMapper

class InMemoryHomeRepository(
    private val rankingPolicy: HomeRankingPolicy = HomeRankingPolicy(),
    initialItems: List<HomeFeedItemDto> = defaultSeedItems
) : HomeRepository {

    private val items = initialItems.map { HomeMapper.toDomain(it) }.toMutableList()
    private val preferencesMap = mutableMapOf<String, UserPreferences>()

    override suspend fun getFeed(userId: String): List<HomeFeedItem> {
        val userPrefs = preferencesMap[userId] ?: UserPreferences()
        return rankingPolicy.rankItems(items, userPrefs)
    }

    override suspend fun getPreferences(userId: String): UserPreferences {
        return preferencesMap[userId] ?: UserPreferences()
    }

    override suspend fun updatePreferences(userId: String, preferences: UserPreferences): Result<Unit> {
        preferencesMap[userId] = preferences
        return Result.Success(Unit)
    }

    companion object {
        private val defaultSeedItems = listOf(
            HomeFeedItemDto(
                id = "home_1",
                title = "CECS 491 Software Engineering Textbook",
                description = "Mint condition hard copy. Essential for Senior Project coursework.",
                price = 45.0,
                category = "TEXTBOOKS"
            ),
            HomeFeedItemDto(
                id = "home_2",
                title = "TI-84 Plus CE Graphing Calculator",
                description = "Used for Calculus and Physics. Comes with charging cable.",
                price = 60.0,
                category = "ELECTRONICS"
            ),
            HomeFeedItemDto(
                id = "home_3",
                title = "Ergonomic Dorm Desk Chair",
                description = "Adjustable height and lumbar support, perfect for study sessions.",
                price = 35.0,
                category = "FURNITURE"
            ),
            HomeFeedItemDto(
                id = "home_4",
                title = "Long Beach State Hoodie (Size L)",
                description = "Official campus bookstore hoodie, black and gold. Worn twice.",
                price = 25.0,
                category = "CLOTHING"
            )
        )
    }
}
