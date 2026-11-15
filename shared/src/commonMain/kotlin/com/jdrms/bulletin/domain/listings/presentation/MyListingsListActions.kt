package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.domain.listings.domain.model.Listing

internal data class MyListingsListActions(
    val onBack: () -> Unit,
    val onEditListing: (Listing) -> Unit,
    val onDeleteListing: (Listing) -> Unit,
    val onMarkSoldListing: (Listing) -> Unit,
    val onRestoreListing: (Listing) -> Unit,
    val onRetryReviewRequestCancellation: () -> Unit
)
