package com.jdrms.bulletin.domain.home.presentation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeScreenTest {
    @Test
    fun prefetch_starts_when_two_cards_remain() {
        assertTrue(shouldPrefetchHomeFeedPage(currentIndex = 7, feedSize = 10, hasMore = true))
        assertFalse(shouldPrefetchHomeFeedPage(currentIndex = 6, feedSize = 10, hasMore = true))
    }

    @Test
    fun prefetch_stops_without_more_pages_or_feed_items() {
        assertFalse(shouldPrefetchHomeFeedPage(currentIndex = 9, feedSize = 10, hasMore = false))
        assertFalse(shouldPrefetchHomeFeedPage(currentIndex = 0, feedSize = 0, hasMore = true))
    }
}
