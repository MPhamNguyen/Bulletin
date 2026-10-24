package com.jdrms.bulletin.domain.home.presentation

import com.jdrms.bulletin.domain.home.domain.model.HomeFeedCategory
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedCursor
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedItem
import com.jdrms.bulletin.domain.home.domain.model.UserPreferences

data class HomeUiState(
    val currentIndex: Int = 0,
    val feed: List<HomeFeedItem> = emptyList(),
    val preferences: UserPreferences = UserPreferences(),
    val selectedCategory: HomeFeedCategory? = null,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val nextCursor: HomeFeedCursor? = null,
    val errorMessage: String? = null
) {
    val isDeckLoading: Boolean get() = isLoading || isLoadingMore
}
