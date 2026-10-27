package com.jdrms.bulletin.domain.home.domain.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedCursor
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedPage
import com.jdrms.bulletin.domain.home.domain.model.UserPreferences

interface HomeRepository {
    suspend fun getFeed(userId: String?, cursor: HomeFeedCursor?, pageSize: Int): HomeFeedPage
    suspend fun getPreferences(userId: String?): UserPreferences
    suspend fun updatePreferences(userId: String, preferences: UserPreferences): Result<Unit>
}
