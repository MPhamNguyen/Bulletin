package com.jdrms.bulletin.domain.profile.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileReviewsSectionTest {
    @Test
    fun reviewExpansionUsesThePublicProfileBatchSizes() {
        assertEquals(10, nextProfileReviewCount(currentCount = 3, totalCount = 24))
        assertEquals(20, nextProfileReviewCount(currentCount = 10, totalCount = 24))
        assertEquals(24, nextProfileReviewCount(currentCount = 20, totalCount = 24))
    }

    @Test
    fun reviewExpansionStopsAtTheAvailableReviewCount() {
        assertEquals(7, nextProfileReviewCount(currentCount = 3, totalCount = 7))
    }
}
