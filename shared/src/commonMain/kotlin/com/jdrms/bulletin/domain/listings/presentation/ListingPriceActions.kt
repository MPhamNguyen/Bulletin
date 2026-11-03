package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.designsystem.normalizeCurrencyReplacement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class ListingPriceActions(private val state: MutableStateFlow<ListingsUiState>) {
    fun changeNewPrice(input: String) {
        when (val result = normalizeCurrencyReplacement(input)) {
            is Result.Success -> state.update {
                it.copy(newPriceCents = result.data, errorMessage = null, successMessage = null)
            }
            is Result.Error -> state.update { it.copy(errorMessage = result.message, successMessage = null) }
        }
    }

    fun changeEditPrice(input: String) {
        when (val result = normalizeCurrencyReplacement(input)) {
            is Result.Success -> state.update {
                it.copy(editPriceCents = result.data, errorMessage = null, successMessage = null)
            }
            is Result.Error -> state.update { it.copy(errorMessage = result.message, successMessage = null) }
        }
    }
}
