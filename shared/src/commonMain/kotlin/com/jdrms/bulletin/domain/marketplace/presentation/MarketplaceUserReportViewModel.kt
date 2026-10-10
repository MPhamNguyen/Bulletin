package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceUserReportReason
import com.jdrms.bulletin.domain.marketplace.application.ReportMarketplaceUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MarketplaceUserReportUiState(
    val reportedUserId: String? = null,
    val selectedReason: MarketplaceUserReportReason? = null,
    val description: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val submitted: Boolean = false
)

class MarketplaceUserReportViewModel(
    private val reportMarketplaceUser: ReportMarketplaceUser
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(MarketplaceUserReportUiState())
    val uiState = mutableUiState.asStateFlow()

    fun begin(reportedUserId: String) {
        mutableUiState.value = MarketplaceUserReportUiState(reportedUserId = reportedUserId)
    }

    fun selectReason(reason: MarketplaceUserReportReason) {
        mutableUiState.update { it.copy(selectedReason = reason, errorMessage = null) }
    }

    fun updateDescription(description: String) {
        if (description.length <= 1_000) {
            mutableUiState.update { it.copy(description = description, errorMessage = null) }
        }
    }

    fun submit() {
        val state = mutableUiState.value
        val userId = state.reportedUserId ?: return
        val reason = state.selectedReason ?: run {
            mutableUiState.update { it.copy(errorMessage = "Choose a reason for your report.") }
            return
        }
        if (state.isSubmitting || state.submitted) return
        mutableUiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                when (val result = reportMarketplaceUser(userId, reason, state.description)) {
                    is Result.Success -> mutableUiState.update {
                        it.copy(isSubmitting = false, submitted = true)
                    }
                    is Result.Error -> mutableUiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = result.exception.message ?: "Unable to submit your report."
                        )
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                mutableUiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = "We couldn't submit your report right now. Please try again."
                    )
                }
            }
        }
    }
}
