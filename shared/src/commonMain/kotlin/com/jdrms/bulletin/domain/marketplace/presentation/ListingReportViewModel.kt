package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.application.ReportMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.domain.model.ListingReportReason
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ListingReportUiState(
    val listingId: String? = null,
    val listingSellerId: String? = null,
    val selectedReason: ListingReportReason? = null,
    val description: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val submitted: Boolean = false
)

class ListingReportViewModel(
    private val reportMarketplaceListing: ReportMarketplaceListing
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ListingReportUiState())
    val uiState = mutableUiState.asStateFlow()
    private var formGeneration = 0

    fun begin(listingId: String, sellerId: String) {
        formGeneration++
        mutableUiState.value = ListingReportUiState(listingId = listingId, listingSellerId = sellerId)
    }

    fun dismiss() {
        if (!mutableUiState.value.isSubmitting) {
            formGeneration++
            mutableUiState.value = ListingReportUiState()
        }
    }

    fun selectReason(reason: ListingReportReason) {
        mutableUiState.update { it.copy(selectedReason = reason, errorMessage = null) }
    }

    fun updateDescription(description: String) {
        if (description.length <= 1_000) {
            mutableUiState.update { it.copy(description = description, errorMessage = null) }
        }
    }

    fun submit() {
        val state = mutableUiState.value
        val listingId = state.listingId ?: return
        val sellerId = state.listingSellerId ?: return
        val reason = state.selectedReason ?: run {
            mutableUiState.update { it.copy(errorMessage = "Choose a reason for your report.") }
            return
        }
        if (state.isSubmitting || state.submitted) return
        val generation = formGeneration
        mutableUiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            submitReport(generation, listingId, sellerId, reason, state.description)
        }
    }

    private suspend fun submitReport(
        generation: Int,
        listingId: String,
        sellerId: String,
        reason: ListingReportReason,
        description: String
    ) {
        try {
            val result = reportMarketplaceListing(MarketplaceItemId(listingId), sellerId, reason, description)
            when (result) {
                is Result.Success -> updateIfCurrent(generation, listingId) {
                    it.copy(isSubmitting = false, submitted = true)
                }
                is Result.Error -> updateIfCurrent(generation, listingId) {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = result.exception.message ?: "Unable to submit your report."
                    )
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            updateIfCurrent(generation, listingId) {
                it.copy(
                    isSubmitting = false,
                    errorMessage = "We couldn't submit your report right now. Please try again."
                )
            }
        }
    }

    private fun updateIfCurrent(
        generation: Int,
        listingId: String,
        update: (ListingReportUiState) -> ListingReportUiState
    ) {
        if (generation == formGeneration && mutableUiState.value.listingId == listingId) {
            mutableUiState.update(update)
        }
    }
}
