package com.jdrms.bulletin.domain.home.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedItem
import com.jdrms.bulletin.domain.home.domain.model.UserPreferences
import com.jdrms.bulletin.domain.home.domain.repository.HomeRepository

class GetPersonalizedFeed(
    private val homeRepository: HomeRepository
) {
    suspend operator fun invoke(userId: String = "student_user"): List<HomeFeedItem> {
        return homeRepository.getFeed(userId)
    }
}

class UpdateUserPreferences(
    private val homeRepository: HomeRepository
) {
    suspend operator fun invoke(userId: String = "student_user", preferences: UserPreferences): Result<Unit> {
        return homeRepository.updatePreferences(userId, preferences)
    }
}
