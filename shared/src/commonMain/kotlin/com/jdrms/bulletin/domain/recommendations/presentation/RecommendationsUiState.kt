package com.jdrms.bulletin.domain.recommendations.presentation

import com.jdrms.bulletin.domain.recommendations.domain.model.Recommendation
import com.jdrms.bulletin.domain.recommendations.domain.model.UserPreferences

data class RecommendationsUiState(
    val feed: List<Recommendation> = emptyList(),
    val userPreferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
