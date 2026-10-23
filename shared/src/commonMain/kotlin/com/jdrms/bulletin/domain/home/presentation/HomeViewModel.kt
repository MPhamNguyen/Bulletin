package com.jdrms.bulletin.domain.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.domain.home.application.GetPersonalizedFeed
import com.jdrms.bulletin.domain.home.application.UpdateUserPreferences
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedCategory
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
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
    private val dismissedIds = mutableSetOf<HomeItemId>()
    private var requestVersion = 0L

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
                    dismissedIds.clear()
                    _uiState.update { it.copy(sessionGeneration = it.sessionGeneration + 1) }
                    currentUserId = userId
                    hasResolvedSession = true
                    loadFeed()
                }
            }
        }
    }

    fun loadFeed() {
        val version = beginFeedRefresh()
        feedJob = viewModelScope.launch {
            try {
                val page = getPersonalizedFeed(currentUserId)
                if (version != requestVersion) return@launch
                _uiState.update {
                    it.copy(
                        feed = page.items.distinctBy { item -> item.id }.filterNot { item -> item.id in dismissedIds },
                        isLoading = false,
                        hasMore = page.nextCursor != null,
                        nextCursor = page.nextCursor
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                if (version == requestVersion) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = FEED_LOAD_ERROR) }
                }
            }
        }
    }

    private fun beginFeedRefresh(): Long {
        requestVersion++
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
        return requestVersion
    }

    fun loadNextPage() {
        val state = _uiState.value
        val cursor = state.nextCursor ?: return
        val version = requestVersion
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        _uiState.update { it.copy(isLoadingMore = true, errorMessage = null) }
        feedJob = viewModelScope.launch {
            try {
                val page = getPersonalizedFeed(currentUserId, cursor)
                if (version != requestVersion) return@launch
                _uiState.update { current ->
                    val existingIds = current.feed.mapTo(mutableSetOf()) { it.id }
                    current.copy(
                        feed = current.feed + page.items
                            .filterNot { it.id in dismissedIds }.filter { existingIds.add(it.id) },
                        isLoadingMore = false,
                        hasMore = page.nextCursor != null,
                        nextCursor = page.nextCursor
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                if (version == requestVersion) {
                    _uiState.update { it.copy(isLoadingMore = false, errorMessage = FEED_LOAD_ERROR) }
                }
            }
        }
    }

    fun passListing(id: HomeItemId, sessionGeneration: Long = _uiState.value.sessionGeneration) {
        if (sessionGeneration != _uiState.value.sessionGeneration || !dismissedIds.add(id)) return
        advanceListing(id, sessionGeneration)
    }

    fun advanceListing(id: HomeItemId, sessionGeneration: Long = _uiState.value.sessionGeneration) {
        if (sessionGeneration != _uiState.value.sessionGeneration) return
        _uiState.update { it.copy(feed = it.feed.filterNot { item -> item.id == id }) }
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
