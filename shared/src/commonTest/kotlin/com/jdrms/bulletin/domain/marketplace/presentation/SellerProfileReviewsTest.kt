package com.jdrms.bulletin.domain.marketplace.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class SellerProfileReviewsTest {
    @Test
    fun firstExpansionShowsUpToSevenAdditionalReviews() {
        assertEquals(10, nextVisibleReviewCount(currentCount = 3, totalCount = 24))
        assertEquals(8, nextVisibleReviewCount(currentCount = 3, totalCount = 8))
    }

    @Test
    fun laterExpansionsShowUpToTenAdditionalReviews() {
        assertEquals(20, nextVisibleReviewCount(currentCount = 10, totalCount = 24))
        assertEquals(24, nextVisibleReviewCount(currentCount = 20, totalCount = 24))
    }
}
