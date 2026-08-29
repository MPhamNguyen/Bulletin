package com.jdrms.bulletin.domain.home.domain.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedItem
import com.jdrms.bulletin.domain.home.domain.model.UserPreferences

interface HomeRepository {
    suspend fun getFeed(userId: String): List<HomeFeedItem>
    suspend fun getPreferences(userId: String): UserPreferences
    suspend fun updatePreferences(userId: String, preferences: UserPreferences): Result<Unit>
}
