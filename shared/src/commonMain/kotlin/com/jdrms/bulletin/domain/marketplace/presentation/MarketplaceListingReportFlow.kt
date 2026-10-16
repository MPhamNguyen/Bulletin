package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

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
