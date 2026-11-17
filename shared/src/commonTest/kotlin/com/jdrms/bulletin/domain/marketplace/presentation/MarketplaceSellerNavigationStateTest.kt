package com.jdrms.bulletin.domain.marketplace.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class MarketplaceSellerNavigationStateTest {
    @Test
    fun aSellerReviewsRequestOpensTheFullReviewsPage() {
        assertEquals(
            MarketplaceDestination.ALL_REVIEWS,
            initialMarketplaceSellerDestination(sellerProfileId = null, sellerReviewsId = "seller-1")
        )
    }

    @Test
    fun aSellerProfileRequestOpensTheSellerProfile() {
        assertEquals(
            MarketplaceDestination.SELLER_PROFILE,
            initialMarketplaceSellerDestination(sellerProfileId = "seller-1", sellerReviewsId = null)
        )
    }

    @Test
    fun noSellerRequestOpensTheCatalog() {
        assertEquals(
            MarketplaceDestination.CATALOG,
            initialMarketplaceSellerDestination(sellerProfileId = null, sellerReviewsId = null)
        )
    }
}
