package com.jdrms.bulletin.domain.recommendations.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.domain.identity.domain.model.UserId
import com.jdrms.bulletin.domain.recommendations.application.GetPersonalizedFeed
import com.jdrms.bulletin.domain.recommendations.application.UpdateUserPreferences
import com.jdrms.bulletin.domain.recommendations.domain.model.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecommendationsViewModel(
    private val getPersonalizedFeed: GetPersonalizedFeed,
    private val updateUserPreferences: UpdateUserPreferences,
    private val currentUserId: UserId = UserId("user_101")
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecommendationsUiState())
    val uiState: StateFlow<RecommendationsUiState> = _uiState.asStateFlow()

    init {
        loadFeed()
    }

    fun loadFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val items = getPersonalizedFeed(currentUserId, _uiState.value.userPreferences)
            _uiState.update { it.copy(feed = items, isLoading = false) }
        }
    }

    fun updatePreferences(newPreferences: UserPreferences) {
        _uiState.update { it.copy(userPreferences = newPreferences) }
        viewModelScope.launch {
            updateUserPreferences(currentUserId, newPreferences)
            loadFeed()
        }
    }
}
