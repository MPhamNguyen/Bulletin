package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.core.common.formatRelativeTime
import kotlin.test.Test
import kotlin.test.assertEquals

class MarketplaceListingTimeTest {
    @Test
    fun nullEditTimeShowsCreationAge() {
        assertEquals(formatRelativeTime(1_000L, 90_000L), formatMarketplaceListingTime(1_000L, null, 90_000L))
    }

    @Test
    fun nonNullEditTimeShowsEditedEvenWithinTheCreationMillisecond() {
        assertEquals(
            "Edited ${formatRelativeTime(1_000L, 90_000L)}",
            formatMarketplaceListingTime(1_000L, 1_000L, 90_000L)
        )
    }
}
