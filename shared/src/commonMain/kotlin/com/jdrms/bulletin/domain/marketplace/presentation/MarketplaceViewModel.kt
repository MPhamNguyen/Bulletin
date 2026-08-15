package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.core.common.generateUuid
import com.jdrms.bulletin.domain.identity.domain.model.UserId
import com.jdrms.bulletin.domain.marketplace.application.CreateListing
import com.jdrms.bulletin.domain.marketplace.application.ManageListing
import com.jdrms.bulletin.domain.marketplace.application.SearchListings
import com.jdrms.bulletin.domain.marketplace.application.ToggleFavorite
import com.jdrms.bulletin.domain.marketplace.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MarketplaceViewModel(
    private val searchListings: SearchListings,
    private val createListing: CreateListing,
    private val manageListing: ManageListing,
    private val toggleFavorite: ToggleFavorite,
    private val currentUserId: UserId = UserId("user_101")
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketplaceUiState())
    val uiState: StateFlow<MarketplaceUiState> = _uiState.asStateFlow()

    init {
        loadListings()
        loadFavorites()
    }

    fun loadListings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val results = searchListings.search(_uiState.value.searchQuery, _uiState.value.selectedCategory)
            _uiState.update { it.copy(listings = results, isLoading = false) }
        }
    }

    fun loadFavorites() {
        viewModelScope.launch {
            val favs = toggleFavorite.getFavorites(currentUserId)
            _uiState.update { it.copy(favoriteIds = favs.toSet()) }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadListings()
    }

    fun onCategorySelected(category: Category?) {
        _uiState.update { it.copy(selectedCategory = category) }
        loadListings()
    }

    fun onListingSelected(listing: Listing?) {
        _uiState.update { it.copy(selectedListing = listing) }
    }

    fun deleteListing(listingId: ListingId) {
        viewModelScope.launch {
            when (val res = manageListing.deleteListing(listingId)) {
                is Result.Success -> loadListings()
                is Result.Error -> _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }

    fun onToggleFavorite(listingId: ListingId) {
        viewModelScope.launch {
            when (val res = toggleFavorite(currentUserId, listingId)) {
                is Result.Success -> loadFavorites()
                is Result.Error -> _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }

    fun showCreateModal(show: Boolean) {
        _uiState.update { it.copy(showCreateDialog = show) }
    }

    fun onNewTitleChanged(title: String) { _uiState.update { it.copy(newTitle = title) } }
    fun onNewDescriptionChanged(desc: String) { _uiState.update { it.copy(newDescription = desc) } }
    fun onNewPriceChanged(price: String) { _uiState.update { it.copy(newPrice = price) } }
    fun onNewCategoryChanged(cat: Category) { _uiState.update { it.copy(newCategory = cat) } }

    fun createNewListing() {
        val title = _uiState.value.newTitle
        val desc = _uiState.value.newDescription
        val priceVal = _uiState.value.newPrice.toDoubleOrNull() ?: 0.0

        val newListing = Listing(
            id = ListingId("item_" + generateUuid()),
            sellerId = SellerId(currentUserId.value),
            sellerName = "Dominic Alfonso",
            title = title,
            description = desc,
            price = Price(priceVal),
            category = _uiState.value.newCategory,
            createdAtMillis = currentTimeMillis()
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val res = createListing(newListing)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            showCreateDialog = false,
                            newTitle = "",
                            newDescription = "",
                            newPrice = "",
                            isLoading = false
                        )
                    }
                    loadListings()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = res.message, isLoading = false) }
                }
            }
        }
    }
}
