package com.jdrms.bulletin.domain.home.presentation

import com.jdrms.bulletin.domain.home.domain.model.Recommendation
import com.jdrms.bulletin.domain.home.domain.model.UserPreferences

data class RecommendationsUiState(
    val feed: List<Recommendation> = emptyList(),
    val userPreferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
