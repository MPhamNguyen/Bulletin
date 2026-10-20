package com.jdrms.bulletin.domain.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.domain.home.application.GetPersonalizedFeed
import com.jdrms.bulletin.domain.home.application.UpdateUserPreferences
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedCategory
import com.jdrms.bulletin.domain.profile.application.SessionRepository
import com.jdrms.bulletin.domain.profile.application.SessionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getPersonalizedFeed: GetPersonalizedFeed,
    private val updateUserPreferences: UpdateUserPreferences,
    private val sessionRepository: SessionRepository,
    private val listingChangedSignal: RefreshSignal? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()
    private var currentUserId: String? = null
    private var hasResolvedSession = false
    private var feedJob: Job? = null

    init {
        listingChangedSignal?.let { signal ->
            viewModelScope.launch {
                signal.events.collect {
                    if (hasResolvedSession) loadFeed()
                }
            }
        }
        viewModelScope.launch {
            sessionRepository.state.collect { session ->
                if (session == SessionState.Checking) return@collect
                val userId = (session as? SessionState.Authenticated)?.profile?.id?.value
                if (!hasResolvedSession || userId != currentUserId) {
                    currentUserId = userId
                    hasResolvedSession = true
                    loadFeed()
                }
            }
        }
    }

    fun loadFeed() {
        feedJob?.cancel()
        _uiState.update {
            it.copy(
                feed = emptyList(),
                isLoading = true,
                isLoadingMore = false,
                hasMore = false,
                nextCursor = null,
                errorMessage = null
            )
        }
        feedJob = viewModelScope.launch {
            try {
                val page = getPersonalizedFeed(currentUserId)
                _uiState.update {
                    it.copy(
                        feed = page.items,
                        isLoading = false,
                        hasMore = page.nextCursor != null,
                        nextCursor = page.nextCursor
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = FEED_LOAD_ERROR) }
            }
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        val cursor = state.nextCursor ?: return
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        _uiState.update { it.copy(isLoadingMore = true, errorMessage = null) }
        feedJob = viewModelScope.launch {
            try {
                val page = getPersonalizedFeed(currentUserId, cursor)
                _uiState.update { current ->
                    val existingIds = current.feed.mapTo(mutableSetOf()) { it.id }
                    current.copy(
                        feed = current.feed + page.items.filterNot { it.id in existingIds },
                        isLoadingMore = false,
                        hasMore = page.nextCursor != null,
                        nextCursor = page.nextCursor
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingMore = false, errorMessage = FEED_LOAD_ERROR) }
            }
        }
    }

    fun onCategoryFilterSelected(category: HomeFeedCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun updatePreferences(preferredCategories: List<HomeFeedCategory>, maxPrice: Double?) {
        val userId = currentUserId
        if (userId == null) {
            _uiState.update { it.copy(errorMessage = PREFERENCES_REQUIRE_ACCOUNT) }
            return
        }
        val newPreferences = _uiState.value.preferences.copy(
            preferredCategories = preferredCategories,
            maxPrice = maxPrice
        )
        viewModelScope.launch {
            val result = updateUserPreferences(userId, newPreferences)
            if (result.isSuccess()) {
                _uiState.update { it.copy(preferences = newPreferences) }
                loadFeed()
            } else {
                _uiState.update { it.copy(errorMessage = PREFERENCES_UPDATE_ERROR) }
            }
        }
    }

    private companion object {
        const val FEED_LOAD_ERROR = "Failed to load listings"
        const val PREFERENCES_REQUIRE_ACCOUNT = "Sign in to update feed preferences"
        const val PREFERENCES_UPDATE_ERROR = "Failed to update preferences"
    }
}
