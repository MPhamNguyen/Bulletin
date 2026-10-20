package com.jdrms.bulletin.domain.home.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedCursor
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedItem
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedPage
import com.jdrms.bulletin.domain.home.domain.model.UserPreferences
import com.jdrms.bulletin.domain.home.domain.repository.HomeRepository
import com.jdrms.bulletin.domain.home.domain.service.HomeRankingPolicy
import com.jdrms.bulletin.domain.home.infrastructure.dto.HomeFeedItemDto
import com.jdrms.bulletin.domain.home.infrastructure.mapper.HomeMapper
import kotlin.time.Instant

class InMemoryHomeRepository(
    private val rankingPolicy: HomeRankingPolicy = HomeRankingPolicy(),
    initialItems: List<HomeFeedItemDto> = emptyList()
) : HomeRepository {

    private val items = initialItems.map { HomeMapper.toDomain(it) }.toMutableList()
    private val preferencesMap = mutableMapOf<String, UserPreferences>()

    override suspend fun getFeed(userId: String?, cursor: HomeFeedCursor?, pageSize: Int): HomeFeedPage {
        require(pageSize > 0)
        val pageItems = items.asSequence()
            .sortedWith(compareByDescending<HomeFeedItem> { it.createdAtMillis }.thenByDescending { it.id.value })
            .filter { item -> cursor == null || item.isAfter(cursor) }
            .take(pageSize + 1)
            .toList()
        val selectedItems = pageItems.take(pageSize)
        val nextCursor = if (pageItems.size > pageSize) {
            selectedItems.lastOrNull()?.let {
                HomeFeedCursor(Instant.fromEpochMilliseconds(it.createdAtMillis), it.id)
            }
        } else {
            null
        }
        val preferences = userId?.let(preferencesMap::get) ?: UserPreferences()
        return HomeFeedPage(rankingPolicy.rankItems(selectedItems, preferences), nextCursor)
    }

    override suspend fun getPreferences(userId: String?): UserPreferences {
        return userId?.let(preferencesMap::get) ?: UserPreferences()
    }

    override suspend fun updatePreferences(userId: String, preferences: UserPreferences): Result<Unit> {
        preferencesMap[userId] = preferences
        return Result.Success(Unit)
    }

    private fun HomeFeedItem.isAfter(cursor: HomeFeedCursor): Boolean =
        createdAtMillis < cursor.createdAt.toEpochMilliseconds() ||
            (createdAtMillis == cursor.createdAt.toEpochMilliseconds() && id.value < cursor.itemId.value)
}
