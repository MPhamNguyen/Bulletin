package com.jdrms.bulletin.domain.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.application.GetPersonalizedFeed
import com.jdrms.bulletin.domain.home.application.RecordSwipeDecision
import com.jdrms.bulletin.domain.home.application.SaveListingPort
import com.jdrms.bulletin.domain.home.application.SendQuickMessage
import com.jdrms.bulletin.domain.home.application.UpdateUserPreferences
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedCategory
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedCursor
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import com.jdrms.bulletin.domain.home.domain.repository.SwipeDecisionRepository
import com.jdrms.bulletin.domain.home.infrastructure.repository.InMemorySwipeDecisionRepository
import com.jdrms.bulletin.domain.profile.application.SessionRepository
import com.jdrms.bulletin.domain.profile.application.SessionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel private constructor(
    private val getPersonalizedFeed: GetPersonalizedFeed,
    private val updateUserPreferences: UpdateUserPreferences,
    private val sessionRepository: SessionRepository,
    private val listingChangedSignal: RefreshSignal?,
    swipeDecisionRepository: SwipeDecisionRepository,
    private val interactions: HomeCardInteraction
) : ViewModel(), HomeCardActions by interactions {
    constructor(
        getPersonalizedFeed: GetPersonalizedFeed,
        updateUserPreferences: UpdateUserPreferences,
        sessionRepository: SessionRepository,
        listingChangedSignal: RefreshSignal? = null,
        swipeDecisionRepository: SwipeDecisionRepository = InMemorySwipeDecisionRepository(),
        recordSwipeDecision: RecordSwipeDecision = RecordSwipeDecision(swipeDecisionRepository),
        sendQuickMessage: SendQuickMessage? = null,
        saveListing: SaveListingPort? = null
    ) : this(
        getPersonalizedFeed,
        updateUserPreferences,
        sessionRepository,
        listingChangedSignal,
        swipeDecisionRepository,
        HomeCardInteraction(swipeDecisionRepository, recordSwipeDecision, sendQuickMessage, saveListing)
    )

    private val _uiState = interactions.store.state
    val uiState = _uiState.asStateFlow()
    private var currentUserId: String? = null
    private var hasResolvedSession = false
    private var feedJob: Job? = null
    private val deck = HomeDeckPolicy(swipeDecisionRepository)
    private var requestGeneration = 0L

    init {
        interactions.store.scope = viewModelScope
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
                    interactions.reset()
                    interactions.store.changeViewer(userId)
                    val cleanup = deck.changeAccount(userId)
                    currentUserId = userId
                    hasResolvedSession = true
                    loadFeed()
                    if (cleanup is Result.Error) {
                        _uiState.update {
                            it.copy(
                                errorMessage = DECISION_CLEANUP_ERROR,
                                errorRetryAction = HomeErrorRetryAction.RELOAD_FEED
                            )
                        }
                    }
                }
            }
        }
    }

    fun advanceCard(expectedIndex: Int, expectedGeneration: Long = _uiState.value.deckGeneration) {
        if (_uiState.value.interaction.card in setOf(CardState.Sending, CardState.Leaving)) return
        _uiState.update { state ->
            if (state.deckGeneration != expectedGeneration || state.currentIndex != expectedIndex ||
                expectedIndex !in state.feed.indices
            ) {
                state
            } else {
                state.copy(currentIndex = expectedIndex + 1, prefetchEnabled = true)
            }
        }
        interactions.store.syncListing()
    }

    internal fun excludeLoadedCard(id: HomeItemId, exclusion: HomeDeckExclusion) {
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            val userId = currentUserId
            deck.exclude(id, exclusion)
            val excluded = deck.excludedIds()
            if (userId != currentUserId) return@launch
            _uiState.update { state ->
                val consumed = deck.eligible(state.feed.take(state.currentIndex), excluded).size
                state.copy(
                    feed = deck.eligible(state.loadedItems, excluded),
                    currentIndex = consumed,
                    deckGeneration = state.deckGeneration + 1
                )
            }
            interactions.store.syncListing()
        }
    }

    fun replayLoadedCards() {
        interactions.reset()
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            val userId = currentUserId
            when (deck.clearPassDecisions()) {
                is Result.Success -> {
                    val excluded = deck.excludedIds()
                    if (userId == currentUserId) {
                        _uiState.update {
                            deck.replay(it, excluded).copy(
                                errorMessage = null,
                                errorRetryAction = HomeErrorRetryAction.RELOAD_FEED
                            )
                        }
                        interactions.store.syncListing()
                    }
                }
                is Result.Error -> if (userId == currentUserId) {
                    _uiState.update {
                        it.copy(
                            errorMessage = REPLAY_DECISIONS_ERROR,
                            errorRetryAction = HomeErrorRetryAction.REPLAY
                        )
                    }
                }
            }
        }
    }

    fun refresh() = loadFeed()

    fun loadFeed() {
        interactions.reset()
        feedJob?.cancel()
        val generation = ++requestGeneration
        deck.resetPagination()
        _uiState.update {
            it.copy(
                loadedItems = emptyList(),
                deckGeneration = it.deckGeneration + 1,
                currentIndex = 0,
                prefetchEnabled = true,
                feed = emptyList(),
                isLoading = true,
                isLoadingMore = false,
                hasMore = false,
                nextCursor = null,
                errorMessage = null,
                errorRetryAction = HomeErrorRetryAction.RELOAD_FEED
            )
        }
        interactions.store.syncListing()
        feedJob = viewModelScope.launch { retrievePages(null, generation, currentUserId) }
    }

    fun loadNextPage() {
        val state = _uiState.value
        val cursor = state.nextCursor ?: return
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        _uiState.update {
            it.copy(
                isLoadingMore = true,
                errorMessage = null,
                errorRetryAction = HomeErrorRetryAction.RELOAD_FEED
            )
        }
        val generation = requestGeneration
        feedJob = viewModelScope.launch { retrievePages(cursor, generation, currentUserId) }
    }

    private suspend fun retrievePages(cursor: HomeFeedCursor?, generation: Long, userId: String?) {
        var next = cursor
        try {
            do {
                val page = getPersonalizedFeed(userId, next)
                val excluded = deck.excludedIds()
                if (generation != requestGeneration) return
                _uiState.update { interactions.store.reconcilePage(it, deck.acceptPage(it, page, excluded)) }
                interactions.store.syncListing()
                next = page.nextCursor
            } while (deck.shouldContinue(_uiState.value))
            _uiState.update { it.copy(isLoading = false, isLoadingMore = false) }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            if (generation == requestGeneration) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = FEED_LOAD_ERROR,
                        errorRetryAction = HomeErrorRetryAction.RELOAD_FEED
                    )
                }
            }
        }
    }

    fun onCategoryFilterSelected(category: HomeFeedCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun updatePreferences(preferredCategories: List<HomeFeedCategory>, maxPrice: Double?) {
        val userId = currentUserId
        if (userId == null) {
            _uiState.update {
                it.copy(
                    errorMessage = PREFERENCES_REQUIRE_ACCOUNT,
                    errorRetryAction = HomeErrorRetryAction.RELOAD_FEED
                )
            }
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
                _uiState.update {
                    it.copy(
                        errorMessage = PREFERENCES_UPDATE_ERROR,
                        errorRetryAction = HomeErrorRetryAction.RELOAD_FEED
                    )
                }
            }
        }
    }

    override fun onCleared() {
        interactions.reset()
        interactions.store.changeViewer(null)
    }

    private companion object {
        const val FEED_LOAD_ERROR = "Failed to load listings"
        const val DECISION_CLEANUP_ERROR = "Could not clear previous deck decisions"
        const val REPLAY_DECISIONS_ERROR = "Could not replay listings. Try again."
        const val PREFERENCES_REQUIRE_ACCOUNT = "Sign in to update feed preferences"
        const val PREFERENCES_UPDATE_ERROR = "Failed to update preferences"
    }
}
