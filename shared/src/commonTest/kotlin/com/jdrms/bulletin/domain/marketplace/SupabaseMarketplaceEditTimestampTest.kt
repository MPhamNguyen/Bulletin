package com.jdrms.bulletin.domain.marketplace

import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.SupabaseMarketplaceListingDto
import com.jdrms.bulletin.domain.marketplace.infrastructure.mapper.SupabaseMarketplaceListingMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SupabaseMarketplaceEditTimestampTest {
    private val listing = SupabaseMarketplaceListingDto(
        id = "listing",
        name = "Desk lamp",
        userId = "seller",
        price = 5.0,
        createdAt = "2026-11-25T00:00:00Z"
    )

    @Test
    fun absentOrInvalidEditTimestampsStayNull() {
        assertNull(SupabaseMarketplaceListingMapper.toItem(listing)?.editedAtMillis)
        assertNull(SupabaseMarketplaceListingMapper.toItem(listing.copy(editedAt = "invalid"))?.editedAtMillis)
    }

    @Test
    fun serverEditTimeIsExposedToCardsAndSnapshots() {
        val edited = listing.copy(editedAt = "1970-01-01T00:00:01Z")
        assertEquals(1_000L, SupabaseMarketplaceListingMapper.toItem(edited)?.editedAtMillis)
        assertEquals(1_000L, SupabaseMarketplaceListingMapper.toSnapshot(edited)?.editedAtMillis)
    }
}
