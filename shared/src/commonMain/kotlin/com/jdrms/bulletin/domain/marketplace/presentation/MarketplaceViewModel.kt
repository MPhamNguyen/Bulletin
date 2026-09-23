package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageRequest
import com.jdrms.bulletin.domain.marketplace.application.SearchMarketplace
import com.jdrms.bulletin.domain.marketplace.application.ToggleSaveMarketplaceItem
import com.jdrms.bulletin.domain.marketplace.application.ViewMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MarketplaceViewModel(
    private val searchMarketplace: SearchMarketplace,
    private val toggleSaveItem: ToggleSaveMarketplaceItem,
    private val viewMarketplaceListing: ViewMarketplaceListing
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketplaceUiState())
    val uiState: StateFlow<MarketplaceUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    init {
        refreshListings()
    }

    fun refreshListings(userId: String = "student_user") {
        loadFirstPage(userId = userId, debounceMillis = 0L)
    }

    private fun loadFirstPage(userId: String, debounceMillis: Long) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    items = emptyList(),
                    isLoading = true,
                    isLoadingMore = false,
                    nextCursor = null,
                    endReached = false,
                    errorMessage = null
                )
            }
            if (debounceMillis > 0L) {
                delay(debounceMillis)
            }
            val query = _uiState.value.searchQuery
            val category = _uiState.value.selectedCategory
            runCatching {
                val page = searchMarketplace.getPage(
                    MarketplacePageRequest(query = query, category = category)
                )
                val savedIds = toggleSaveItem.getSavedIds(userId)
                page to savedIds
            }.fold(
                onSuccess = { (page, savedIds) ->
                    _uiState.update { state ->
                        if (state.searchQuery == query && state.selectedCategory == category) {
                            state.copy(
                                items = page.items,
                                savedItemIds = savedIds,
                                isLoading = false,
                                nextCursor = page.nextCursor,
                                endReached = page.nextCursor == null
                            )
                        } else {
                            state
                        }
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            errorMessage = "Unable to load marketplace listings. Please try again."
                        )
                    }
                }
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadFirstPage(userId = DEFAULT_USER_ID, debounceMillis = SEARCH_DEBOUNCE_MILLIS)
    }

    fun onCategorySelected(category: MarketplaceCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
        loadFirstPage(userId = DEFAULT_USER_ID, debounceMillis = 0L)
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        val cursor = currentState.nextCursor ?: return
        if (currentState.isLoading || currentState.isLoadingMore || currentState.endReached) return

        _uiState.update { it.copy(isLoadingMore = true, errorMessage = null) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val query = currentState.searchQuery
            val category = currentState.selectedCategory
            runCatching {
                searchMarketplace.getPage(
                    MarketplacePageRequest(
                        query = query,
                        category = category,
                        cursor = cursor
                    )
                )
            }.fold(
                onSuccess = { page ->
                    _uiState.update { state ->
                        if (state.searchQuery == query && state.selectedCategory == category) {
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
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            errorMessage = "Unable to load more listings. Please try again."
                        )
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
            it.copy(
                selectedListingId = listingId,
                selectedListing = null,
                isDetailLoading = true,
                detailErrorMessage = null,
                isDetailSheetOpen = true
            )
        }
        loadListingDetail(listingId)
    }

    fun retryLoadListingDetail() {
        val listingId = _uiState.value.selectedListingId ?: return
        _uiState.update {
            it.copy(isDetailLoading = true, detailErrorMessage = null)
        }
        loadListingDetail(listingId)
    }

    private fun loadListingDetail(listingId: String) {
        viewModelScope.launch {
            when (val result = viewMarketplaceListing(listingId)) {
                is com.jdrms.bulletin.core.common.Result.Success -> {
                    val listing = result.data
                    val isSaved = _uiState.value.savedItemIds.contains(listing.id)
                    _uiState.update {
                        it.copy(
                            selectedListing = listing.copy(isSaved = isSaved),
                            isDetailLoading = false,
                            detailErrorMessage = null
                        )
                    }
                }
                is com.jdrms.bulletin.core.common.Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isDetailLoading = false,
                            detailErrorMessage = result.exception.message ?: "Failed to load listing details."
                        )
                    }
                }
            }
        }
    }

    fun dismissListingDetail() {
        _uiState.update {
            it.copy(
                isDetailSheetOpen = false,
                selectedListing = null,
                selectedListingId = null,
                detailErrorMessage = null,
                isDetailLoading = false
            )
        }
    }

    fun toggleSaved(itemId: MarketplaceItemId, userId: String = "student_user") {
        viewModelScope.launch {
            val result = toggleSaveItem(userId, itemId)
            if (result.isSuccess()) {
                val savedIds = toggleSaveItem.getSavedIds(userId)
                _uiState.update { state ->
                    val updatedListing = if (state.selectedListing?.id == itemId) {
                        state.selectedListing.copy(isSaved = savedIds.contains(itemId))
                    } else {
                        state.selectedListing
                    }
                    state.copy(savedItemIds = savedIds, selectedListing = updatedListing)
                }
            }
        }
    }

    private companion object {
        const val DEFAULT_USER_ID = "student_user"
        const val SEARCH_DEBOUNCE_MILLIS = 300L
    }
}
