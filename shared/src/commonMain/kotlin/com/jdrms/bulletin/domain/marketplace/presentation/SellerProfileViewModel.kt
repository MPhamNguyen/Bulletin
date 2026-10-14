package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceSellerProfile
import com.jdrms.bulletin.domain.marketplace.application.ViewMarketplaceSellerProfile
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SellerProfileUiState(
    val profile: MarketplaceSellerProfile? = null,
    val listings: List<MarketplaceItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class SellerProfileViewModel(
    private val viewSellerProfile: ViewMarketplaceSellerProfile
) : ViewModel() {
    private val _uiState = MutableStateFlow(SellerProfileUiState())
    val uiState: StateFlow<SellerProfileUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    fun load(sellerId: String) {
        loadJob?.cancel()
        _uiState.value = SellerProfileUiState(isLoading = true)
        loadJob = viewModelScope.launch {
            try {
                val details = viewSellerProfile(sellerId)
                _uiState.update {
                    if (details == null) {
                        SellerProfileUiState(errorMessage = "Seller profile is currently unavailable.")
                    } else {
                        SellerProfileUiState(profile = details.profile, listings = details.listings)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.value = SellerProfileUiState(
                    errorMessage = "Unable to load the seller profile. Please try again."
                )
            }
        }
    }
}
