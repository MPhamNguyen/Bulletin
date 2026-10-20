package com.jdrms.bulletin.domain.home.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedCursor
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedPage
import com.jdrms.bulletin.domain.home.domain.model.UserPreferences
import com.jdrms.bulletin.domain.home.domain.repository.HomeRepository

class GetPersonalizedFeed(
    private val homeRepository: HomeRepository
) {
    suspend operator fun invoke(
        userId: String?,
        cursor: HomeFeedCursor? = null,
        pageSize: Int = HOME_FEED_PAGE_SIZE
    ): HomeFeedPage {
        return homeRepository.getFeed(userId, cursor, pageSize)
    }
}

class UpdateUserPreferences(
    private val homeRepository: HomeRepository
) {
    suspend operator fun invoke(userId: String, preferences: UserPreferences): Result<Unit> {
        return homeRepository.updatePreferences(userId, preferences)
    }
}

const val HOME_FEED_PAGE_SIZE = 10
