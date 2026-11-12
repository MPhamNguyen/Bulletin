package com.jdrms.bulletin.domain.marketplace.application

data class MarketplaceSellerReviewPage(
    val reviews: List<MarketplaceSellerReview>,
    val nextOffset: Int?
)

const val SELLER_REVIEWS_PAGE_SIZE = 20
