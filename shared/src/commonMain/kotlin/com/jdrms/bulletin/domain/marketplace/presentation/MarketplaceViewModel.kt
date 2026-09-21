package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.domain.marketplace.application.SearchMarketplace
import com.jdrms.bulletin.domain.marketplace.application.ToggleSaveMarketplaceItem
import com.jdrms.bulletin.domain.marketplace.application.ViewMarketplaceListing
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
    private val toggleSaveItem: ToggleSaveMarketplaceItem,
    private val viewMarketplaceListing: ViewMarketplaceListing
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
}
