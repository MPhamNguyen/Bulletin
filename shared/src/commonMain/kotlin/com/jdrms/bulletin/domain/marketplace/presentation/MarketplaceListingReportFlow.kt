package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing

internal fun beginMarketplaceListingReport(viewModel: ListingReportViewModel, listing: Listing?) {
    listing?.let { viewModel.begin(it.id.value, it.sellerId) }
}

internal fun handleMarketplaceListingReportDismissal(
    state: ListingReportUiState,
    dismissForm: () -> Unit,
    onReportDone: () -> Unit
) {
    if (state.submitted) onReportDone() else dismissForm()
}

@Composable
internal fun MarketplaceListingReportFlow(
    viewModel: ListingReportViewModel,
    onReportDone: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    if (state.listingId != null) {
        ReportMarketplaceListingScreen(
            state = state,
            onReasonSelected = viewModel::selectReason,
            onDescriptionChanged = viewModel::updateDescription,
            onSubmit = viewModel::submit,
            onDismiss = {
                handleMarketplaceListingReportDismissal(state, viewModel::dismiss, onReportDone)
            }
        )
    }
}
