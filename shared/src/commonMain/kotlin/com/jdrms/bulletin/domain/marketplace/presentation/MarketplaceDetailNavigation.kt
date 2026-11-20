package com.jdrms.bulletin.domain.marketplace.presentation

/** Navigation callbacks for the listing detail flow. */
internal data class MarketplaceDetailNavigation(
    val onNavigate: (MarketplaceDestination) -> Unit,
    val onDismiss: () -> Unit
)
