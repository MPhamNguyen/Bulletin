package com.jdrms.bulletin.core.common

import kotlin.test.Test
import kotlin.test.assertEquals

class RelativeTimeTest {
    @Test
    fun formats_recent_timestamps_readably() {
        val now = 10 * 24 * 60 * 60 * 1_000L

        assertEquals("just now", formatRelativeTime(now - 30_000L, now))
        assertEquals("2 mins ago", formatRelativeTime(now - 2 * 60_000L, now))
        assertEquals("3 hours ago", formatRelativeTime(now - 3 * 60 * 60_000L, now))
        assertEquals("5 days ago", formatRelativeTime(now - 5 * 24 * 60 * 60_000L, now))
    }

    @Test
    fun formats_longer_timestamps_with_months_and_years() {
        val now = 400 * 24 * 60 * 60 * 1_000L

        assertEquals("2 months ago", formatRelativeTime(now - 2 * 30 * 24 * 60 * 60_000L, now))
        assertEquals("1 year ago", formatRelativeTime(now - 365 * 24 * 60 * 60_000L, now))
    }

    @Test
    fun handles_missing_timestamps_without_inventing_an_age() {
        assertEquals("Date unavailable", formatRelativeTime(0L, 1_000L))
    }
}
