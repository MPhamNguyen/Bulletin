package com.jdrms.bulletin.domain.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.domain.home.application.GetPersonalizedFeed
import com.jdrms.bulletin.domain.home.application.UpdateUserPreferences
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getPersonalizedFeed: GetPersonalizedFeed,
    private val updateUserPreferences: UpdateUserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadFeed()
    }

    fun loadFeed(userId: String = "student_user") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val feedItems = getPersonalizedFeed(userId)
            _uiState.update { it.copy(feed = feedItems, isLoading = false) }
        }
    }

    fun onCategoryFilterSelected(category: HomeFeedCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun updatePreferences(preferredCategories: List<HomeFeedCategory>, maxPrice: Double?) {
        val newPrefs = _uiState.value.preferences.copy(
            preferredCategories = preferredCategories,
            maxPrice = maxPrice
        )
        viewModelScope.launch {
            val result = updateUserPreferences(preferences = newPrefs)
            if (result.isSuccess()) {
                _uiState.update { it.copy(preferences = newPrefs) }
                loadFeed()
            } else {
                _uiState.update { it.copy(errorMessage = "Failed to update preferences") }
            }
        }
    }
}
