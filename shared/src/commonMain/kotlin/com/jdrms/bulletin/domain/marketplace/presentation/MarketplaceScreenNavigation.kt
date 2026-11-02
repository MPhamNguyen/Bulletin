package com.jdrms.bulletin.domain.marketplace.presentation

data class MarketplaceScreenNavigation(
    val sellerProfileIdToOpen: String? = null,
    val onSellerProfileRequestHandled: () -> Unit = {},
    val onReturnToInboxChat: () -> Unit = {},
    val userReportIdToOpen: String? = null,
    val onUserReportRequestHandled: () -> Unit = {},
    val onContentNoLongerScrollable: () -> Unit = {}
)
