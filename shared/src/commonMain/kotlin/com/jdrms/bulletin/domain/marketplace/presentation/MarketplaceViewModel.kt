package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.domain.marketplace.application.SearchMarketplace
import com.jdrms.bulletin.domain.marketplace.application.ToggleSaveMarketplaceItem
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
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

    init {
        loadCatalog()
    }

    fun loadCatalog(userId: String = "student_user") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val items = searchMarketplace.getCatalog()
            val savedIds = toggleSaveItem.getSavedIds(userId)
            _uiState.update { it.copy(items = items, savedItemIds = savedIds, isLoading = false) }
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
        viewModelScope.launch {
            val query = _uiState.value.searchQuery
            val category = _uiState.value.selectedCategory
            val results = searchMarketplace.search(query, category)
            _uiState.update { it.copy(items = results) }
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
