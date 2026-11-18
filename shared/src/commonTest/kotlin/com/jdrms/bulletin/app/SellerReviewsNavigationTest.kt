package com.jdrms.bulletin.app

import com.jdrms.bulletin.app.navigation.AppDestination
import com.jdrms.bulletin.app.navigation.MutableMainNavigationState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SellerReviewsNavigationTest {
    @Test
    fun openingSellerReviewsFromProfileRequestsTheMarketplaceReviewsRoute() {
        val navigation = MutableMainNavigationState()

        navigation.openSellerReviews("seller-1")

        assertEquals(AppDestination.MARKETPLACE, navigation.currentDestination)
        assertEquals("seller-1", navigation.pendingSellerReviewsId)
        assertNull(navigation.pendingSellerProfileId)
    }
}
