package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceLayoutPreference
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceLayoutPreferenceStore
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageRequest
import com.jdrms.bulletin.domain.marketplace.application.SearchMarketplace
import com.jdrms.bulletin.domain.marketplace.application.SubmitMarketplaceReview
import com.jdrms.bulletin.domain.marketplace.application.ViewMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCondition
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MarketplaceViewModel(
    private val searchMarketplace: SearchMarketplace,
    bookmarks: MarketplaceBookmarkDependencies,
    private val viewMarketplaceListing: ViewMarketplaceListing,
    private val submitMarketplaceReview: SubmitMarketplaceReview =
        SubmitMarketplaceReview(
            com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryMarketplaceRepository()
        ),
    private val listingChangedSignal: RefreshSignal? = null,
    private val currentUserIdProvider: suspend () -> String? = { null },
    private val layoutPreferenceStore: MarketplaceLayoutPreferenceStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketplaceUiState())
    val uiState: StateFlow<MarketplaceUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null
    private val firstPageLoader = MarketplaceFirstPageLoader(
        _uiState,
        searchMarketplace,
        bookmarks,
        currentUserIdProvider
    )
    private val bookmarkController = MarketplaceBookmarkController(
        _uiState,
        viewModelScope,
        bookmarks,
        currentUserIdProvider
    )
    private val layoutPreferenceController = MarketplaceLayoutPreferenceController(
        _uiState,
        viewModelScope,
        layoutPreferenceStore
    )
    val reviewActions = MarketplaceReviewController(_uiState, submitMarketplaceReview, viewModelScope)

    init {
        layoutPreferenceController.load()
        refreshListings()
        viewModelScope.launch {
            listingChangedSignal?.events?.collect {
                refreshListings()
                _uiState.value.detail.listingId?.let(::loadListingDetail)
            }
        }
    }

    fun onLayoutPreferenceChanged(preference: MarketplaceLayoutPreference) =
        layoutPreferenceController.change(preference)

    fun refreshListings(userId: String? = null) {
        loadFirstPage(userId = userId, debounceMillis = 0L)
    }

    private fun loadFirstPage(userId: String?, debounceMillis: Long) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            firstPageLoader.load(userId, debounceMillis)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(filters = it.filters.copy(query = query)) }
        loadFirstPage(userId = null, debounceMillis = SEARCH_DEBOUNCE_MILLIS)
    }

    fun onCategorySelected(category: MarketplaceCategory?) {
        _uiState.update { it.copy(filters = it.filters.copy(category = category).normalized()) }
        loadFirstPage(userId = null, debounceMillis = 0L)
    }

    fun onConditionSelected(condition: MarketplaceCondition?) {
        _uiState.update { it.copy(filters = it.filters.copy(condition = condition)) }
        loadFirstPage(userId = null, debounceMillis = 0L)
    }

    fun onFiltersApplied(filters: MarketplaceFilterState) {
        _uiState.update { state -> state.copy(filters = filters.copy(query = state.filters.query).normalized()) }
        loadFirstPage(userId = null, debounceMillis = 0L)
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        val cursor = currentState.nextCursor ?: return
        if (currentState.isLoading || currentState.isLoadingMore || currentState.endReached) return

        _uiState.update { it.copy(isLoadingMore = true, errorMessage = null) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val filters = currentState.filters
            runCatching {
                searchMarketplace.getPage(
                    MarketplacePageRequest(
                        query = filters.query,
                        category = filters.category,
                        condition = filters.condition,
                        attributeFilters = filters.attributes,
                        viewerId = currentUserIdProvider(),
                        cursor = cursor
                    )
                )
            }.fold(
                onSuccess = { page ->
                    _uiState.update { state ->
                        if (
                            state.filters == filters &&
                            state.nextCursor == cursor
                        ) {
                            state.copy(
                                items = (state.items + page.items).distinctBy { it.id },
                                isLoadingMore = false,
                                nextCursor = page.nextCursor,
                                endReached = page.nextCursor == null
                            )
                        } else {
                            state
                        }
                    }
                },
                onFailure = { error ->
                    error.rethrowCancellation()
                    _uiState.update { state ->
                        if (
                            state.filters == filters &&
                            state.nextCursor == cursor
                        ) {
                            state.copy(
                                isLoadingMore = false,
                                errorMessage = "Unable to load more listings. Please try again."
                            )
                        } else {
                            state
                        }
                    }
                }
            )
        }
    }

    fun retryListings() {
        if (_uiState.value.items.isEmpty()) {
            refreshListings()
        } else {
            loadNextPage()
        }
    }

    fun onListingClicked(listingId: String) {
        _uiState.update {
            it.copy(detail = MarketplaceDetailState(listingId = listingId, isLoading = true, isOpen = true))
        }
        loadListingDetail(listingId)
    }

    fun retryLoadListingDetail() {
        val listingId = _uiState.value.detail.listingId ?: return
        _uiState.update {
            it.copy(detail = it.detail.copy(isLoading = true, errorMessage = null))
        }
        loadListingDetail(listingId)
    }

    private fun loadListingDetail(listingId: String) {
        viewModelScope.launch {
            when (val result = viewMarketplaceListing(listingId, currentUserIdProvider())) {
                is com.jdrms.bulletin.core.common.Result.Success -> {
                    val listing = result.data
                    val canonicalId = MarketplaceItemId(listingId)
                    val isBookmarked = _uiState.value.bookmarkedItemIds.contains(canonicalId)
                    val currentUserId = currentUserIdProvider()
                    val hasReview = currentUserId?.let {
                        viewMarketplaceListing.hasReview(listing.id.value, it)
                    } ?: false
                    _uiState.update {
                        it.copy(
                            detail = it.detail.copy(
                                listing = listing.copy(id = canonicalId, isBookmarked = isBookmarked),
                                isLoading = false,
                                errorMessage = null,
                                hasReview = hasReview
                            ),
                            currentUserId = currentUserId
                        )
                    }
                }
                is com.jdrms.bulletin.core.common.Result.Error -> {
                    _uiState.update {
                        it.copy(
                            detail = it.detail.copy(
                                isLoading = false,
                                errorMessage = result.exception.message ?: "Failed to load listing details."
                            )
                        )
                    }
                }
            }
        }
    }

    fun dismissListingDetail() {
        _uiState.update { it.copy(detail = MarketplaceDetailState()) }
    }

    fun toggleBookmark(itemId: MarketplaceItemId, userId: String? = null) = bookmarkController.toggle(itemId, userId)

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 300L
    }
}
