package com.jdrms.bulletin.domain.marketplace.presentation

internal data class ListingDetailActionCallbacks(
    val onToggleBookmark: () -> Unit,
    val onDismiss: () -> Unit,
    val onReportListing: () -> Unit
)
