package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.domain.marketplace.application.SearchMarketplace
import com.jdrms.bulletin.domain.marketplace.application.ToggleSaveMarketplaceItem
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MarketplaceViewModel(
    private val searchMarketplace: SearchMarketplace,
    private val toggleSaveItem: ToggleSaveMarketplaceItem
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketplaceUiState())
    val uiState: StateFlow<MarketplaceUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null
    private var catalog: List<MarketplaceItem> = emptyList()

    init {
        refreshListings()
    }

    fun refreshListings(userId: String = "student_user") {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val loadedCatalog = searchMarketplace.getCatalog()
                val savedIds = toggleSaveItem.getSavedIds(userId)
                loadedCatalog to savedIds
            }.fold(
                onSuccess = { (loadedCatalog, savedIds) ->
                    catalog = loadedCatalog
                    _uiState.update { state ->
                        state.copy(
                            items = searchMarketplace.filterCatalog(
                                catalog = loadedCatalog,
                                query = state.searchQuery,
                                category = state.selectedCategory
                            ),
                            savedItemIds = savedIds,
                            isLoading = false
                        )
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Unable to load marketplace listings. Please try again."
                        )
                    }
                }
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applySearch()
    }

    fun onCategorySelected(category: MarketplaceCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
        applySearch()
    }

    private fun applySearch() {
        val query = _uiState.value.searchQuery
        val category = _uiState.value.selectedCategory
        val results = searchMarketplace.filterCatalog(catalog, query, category)
        _uiState.update { state ->
            if (state.searchQuery == query && state.selectedCategory == category) {
                state.copy(items = results, isLoading = false)
            } else {
                state
            }
        }
    }

    fun toggleSaved(itemId: MarketplaceItemId, userId: String = "student_user") {
        viewModelScope.launch {
            val result = toggleSaveItem(userId, itemId)
            if (result.isSuccess()) {
                val savedIds = toggleSaveItem.getSavedIds(userId)
                _uiState.update { it.copy(savedItemIds = savedIds) }
            }
        }
    }
}
