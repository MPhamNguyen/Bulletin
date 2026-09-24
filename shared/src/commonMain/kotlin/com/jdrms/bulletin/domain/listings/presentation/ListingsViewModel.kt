package com.jdrms.bulletin.domain.listings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.core.common.generateUuid
import com.jdrms.bulletin.domain.listings.application.CreateListing
import com.jdrms.bulletin.domain.listings.application.CreateListingErrorMessages
import com.jdrms.bulletin.domain.listings.application.CurrentListingSellerProvider
import com.jdrms.bulletin.domain.listings.application.GetSellerListings
import com.jdrms.bulletin.domain.listings.application.ListingSeller
import com.jdrms.bulletin.domain.listings.application.ManageListing
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingCondition
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.ListingPrice
import com.jdrms.bulletin.domain.listings.domain.model.ListingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ListingsViewModel(
    private val createListing: CreateListing,
    private val manageListing: ManageListing,
    private val getSellerListings: GetSellerListings,
    private val currentSellerProvider: CurrentListingSellerProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListingsUiState())
    val uiState: StateFlow<ListingsUiState> = _uiState.asStateFlow()

    init {
        loadMyListings()
    }

    fun loadMyListings(seller: ListingSeller? = null) {
        viewModelScope.launch {
            when (val sellerResult = seller?.let { Result.Success(it) } ?: currentSellerProvider.getCurrentSeller()) {
                is Result.Success -> {
                    val listings = getSellerListings(sellerResult.data.id)
                    _uiState.update { it.copy(myListings = listings) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = CreateListingErrorMessages.toUserMessage(sellerResult.exception))
                    }
                }
            }
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
        val draft = buildListingDraft() ?: return
        while (true) {
            val state = _uiState.value
            if (state.isSubmitting) return
            if (_uiState.compareAndSet(state, state.copy(isSubmitting = true, errorMessage = null))) break
        }
        viewModelScope.launch { submitListing(draft) }
    }

    private fun buildListingDraft(): NewListingDraft? {
        val state = _uiState.value
        val parsedPrice = state.newPrice.toDoubleOrNull()
        if (parsedPrice == null || !parsedPrice.isFinite() || parsedPrice < 0.0) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid price ($ >= 0)") }
            return null
        }

        val title = state.newTitle.trim()
        if (title.length < 3) {
            _uiState.update { it.copy(errorMessage = "Title must be at least 3 characters") }
            return null
        }

        val description = state.newDescription.trim()
        if (description.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Description cannot be empty") }
            return null
        }

        return NewListingDraft(title, description, parsedPrice, state.newCategory, state.newCondition)
    }

    private suspend fun submitListing(draft: NewListingDraft) {
        when (val sellerResult = currentSellerProvider.getCurrentSeller()) {
            is Result.Success -> {
                val seller = sellerResult.data
                val listing = Listing(
                    id = ListingId(generateUuid()),
                    sellerId = seller.id,
                    sellerName = seller.name,
                    title = draft.title,
                    description = draft.description,
                    price = ListingPrice(draft.price),
                    category = draft.category,
                    condition = draft.condition,
                    status = ListingStatus.AVAILABLE,
                    createdAtMillis = currentTimeMillis()
                )
                when (val result = createListing(listing)) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                newTitle = "",
                                newDescription = "",
                                newPrice = "",
                                isSubmitting = false,
                                successMessage = "Listing posted successfully!"
                            )
                        }
                        loadMyListings(seller)
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = CreateListingErrorMessages.toUserMessage(result.exception)
                        )
                    }
                }
            }
            is Result.Error -> _uiState.update {
                it.copy(
                    isSubmitting = false,
                    errorMessage = CreateListingErrorMessages.toUserMessage(sellerResult.exception)
                )
            }
        }
    }

    private data class NewListingDraft(
        val title: String,
        val description: String,
        val price: Double,
        val category: ListingCategory,
        val condition: ListingCondition
    )

    fun deleteListing(id: ListingId) {
        viewModelScope.launch {
            when (val sellerResult = currentSellerProvider.getCurrentSeller()) {
                is Result.Success -> {
                    if (manageListing.deleteListing(id).isSuccess()) loadMyListings()
                }
                is Result.Error -> _uiState.update {
                    it.copy(errorMessage = CreateListingErrorMessages.toUserMessage(sellerResult.exception))
                }
            }
        }
    }
}
