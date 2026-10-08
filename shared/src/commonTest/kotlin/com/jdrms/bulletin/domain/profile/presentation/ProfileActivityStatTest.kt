package com.jdrms.bulletin.domain.profile.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileActivityStatTest {
    @Test
    fun unknownValuesDisplayAsUnavailable() {
        assertEquals("—", formatProfileStat(null))
    }

    @Test
    fun knownCountsAndRatingsKeepTheirValues() {
        assertEquals("0", formatProfileStat(0))
        assertEquals("4.8", formatProfileStat(4.8))
    }
}
