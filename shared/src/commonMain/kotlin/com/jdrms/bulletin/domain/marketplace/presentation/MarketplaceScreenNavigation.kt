package com.jdrms.bulletin.domain.marketplace.presentation

data class MarketplaceScreenNavigation(
    val sellerProfilePreview: SellerProfilePreview? = null,
    val scrollToTopRequest: Int = 0,
    val sellerProfileIdToOpen: String? = null,
    val sellerReviewsIdToOpen: String? = null,
    val onSellerProfileRequestHandled: () -> Unit = {},
    val onSellerReviewsRequestHandled: () -> Unit = {},
    val onReturnToInboxChat: () -> Unit = {},
    val userReportIdToOpen: String? = null,
    val onUserReportRequestHandled: () -> Unit = {},
    val listingIdToOpen: String? = null,
    val onListingRequestHandled: () -> Unit = {},
    val onListingClosed: () -> Unit = {},
    val onContentNoLongerScrollable: () -> Unit = {}
)
