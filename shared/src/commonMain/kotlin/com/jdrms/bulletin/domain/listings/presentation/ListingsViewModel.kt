package com.jdrms.bulletin.domain.listings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.core.common.generateUuid
import com.jdrms.bulletin.domain.listings.application.CreateListing
import com.jdrms.bulletin.domain.listings.application.GetSellerListings
import com.jdrms.bulletin.domain.listings.application.ManageListing
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingCondition
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.ListingPrice
import com.jdrms.bulletin.domain.listings.domain.model.ListingStatus
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ListingsViewModel(
    private val createListing: CreateListing,
    private val manageListing: ManageListing,
    private val getSellerListings: GetSellerListings,
    private val currentSellerId: SellerId = SellerId("current_student"),
    private val currentSellerName: String = "Dominic Alfonso"
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListingsUiState())
    val uiState: StateFlow<ListingsUiState> = _uiState.asStateFlow()

    init {
        loadMyListings()
    }

    fun loadMyListings() {
        viewModelScope.launch {
            val listings = getSellerListings(currentSellerId)
            _uiState.update { it.copy(myListings = listings) }
        }
    }

    fun onTitleChanged(title: String) {
        _uiState.update { it.copy(newTitle = title, errorMessage = null, successMessage = null) }
    }

    fun onDescriptionChanged(description: String) {
        _uiState.update { it.copy(newDescription = description, errorMessage = null, successMessage = null) }
    }

    fun onPriceChanged(price: String) {
        _uiState.update { it.copy(newPrice = price, errorMessage = null, successMessage = null) }
    }

    fun onCategorySelected(category: ListingCategory) {
        _uiState.update { it.copy(newCategory = category) }
    }

    fun onConditionSelected(condition: ListingCondition) {
        _uiState.update { it.copy(newCondition = condition) }
    }

    fun submitNewListing() {
        val state = _uiState.value
        val parsedPrice = state.newPrice.toDoubleOrNull()

        if (parsedPrice == null || parsedPrice < 0.0) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid price ($ ≥ 0)") }
            return
        }

        if (state.newTitle.isBlank() || state.newTitle.length < 3) {
            _uiState.update { it.copy(errorMessage = "Title must be at least 3 characters") }
            return
        }

        if (state.newDescription.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Description cannot be empty") }
            return
        }

        val newListing = Listing(
            id = ListingId("list_${generateUuid().take(8)}"),
            sellerId = currentSellerId,
            sellerName = currentSellerName,
            title = state.newTitle.trim(),
            description = state.newDescription.trim(),
            price = ListingPrice(parsedPrice),
            category = state.newCategory,
            condition = state.newCondition,
            status = ListingStatus.AVAILABLE,
            createdAtMillis = currentTimeMillis()
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = createListing(newListing)
            if (result.isSuccess()) {
                _uiState.update {
                    it.copy(
                        newTitle = "",
                        newDescription = "",
                        newPrice = "",
                        isSubmitting = false,
                        successMessage = "Listing posted successfully!"
                    )
                }
                loadMyListings()
            } else {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = "Failed to create listing"
                    )
                }
            }
        }
    }

    fun deleteListing(id: ListingId) {
        viewModelScope.launch {
            val result = manageListing.deleteListing(id)
            if (result.isSuccess()) {
                loadMyListings()
            }
        }
    }
}
