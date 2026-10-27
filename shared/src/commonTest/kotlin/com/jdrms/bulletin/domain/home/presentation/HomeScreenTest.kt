package com.jdrms.bulletin.domain.home.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeScreenTest {
    @Test
    fun exhausted_deck_remains_loading_during_append() {
        assertTrue(HomeUiState(isLoading = false, isLoadingMore = true).isDeckLoading)
        assertFalse(HomeUiState(isLoading = false, isLoadingMore = false).isDeckLoading)
    }

    @Test
    fun photo_index_stays_within_the_listing_photo_bounds() {
        assertEquals(0, stepHomePhotoIndex(currentIndex = 0, direction = -1, photoCount = 3))
        assertEquals(1, stepHomePhotoIndex(currentIndex = 0, direction = 1, photoCount = 3))
        assertEquals(2, stepHomePhotoIndex(currentIndex = 2, direction = 1, photoCount = 3))
        assertEquals(0, stepHomePhotoIndex(currentIndex = 0, direction = 1, photoCount = 0))
    }

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
