package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing

internal fun beginMarketplaceListingReport(viewModel: ListingReportViewModel, listing: Listing?) {
    listing?.let { viewModel.begin(it.id.value, it.sellerId) }
}

@Composable
internal fun MarketplaceListingReportFlow(
    viewModel: ListingReportViewModel,
    onReportSubmitted: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(state.submitted) {
        if (state.submitted) onReportSubmitted()
    }
    if (state.listingId != null) {
        ReportMarketplaceListingScreen(
            state = state,
            onReasonSelected = viewModel::selectReason,
            onDescriptionChanged = viewModel::updateDescription,
            onSubmit = viewModel::submit,
            onDismiss = viewModel::dismiss
        )
    }
}
