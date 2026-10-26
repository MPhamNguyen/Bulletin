package com.jdrms.bulletin.core.common

import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ConversationLocalDayKeyTest {
    @Test
    fun usesDeviceLocalCalendarDateAcrossUtcMidnight() {
        val previousZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))

            val beforeUtcMidnight = 1_792_971_000_000L // 2026-10-25 23:30 UTC
            val afterUtcMidnight = 1_792_974_600_000L // 2026-10-26 00:30 UTC
            assertEquals(
                conversationLocalDayKey(beforeUtcMidnight),
                conversationLocalDayKey(afterUtcMidnight)
            )
            assertNotEquals(
                conversationLocalDayKey(beforeUtcMidnight),
                conversationLocalDayKey(1_793_002_800_000L) // 2026-10-26 08:20 UTC
            )
        } finally {
            TimeZone.setDefault(previousZone)
        }
    }
}
