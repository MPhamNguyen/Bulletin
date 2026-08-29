package com.jdrms.bulletin.domain.home.presentation

import com.jdrms.bulletin.domain.home.domain.model.HomeFeedCategory
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedItem
import com.jdrms.bulletin.domain.home.domain.model.UserPreferences

data class HomeUiState(
    val feed: List<HomeFeedItem> = emptyList(),
    val preferences: UserPreferences = UserPreferences(),
    val selectedCategory: HomeFeedCategory? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
