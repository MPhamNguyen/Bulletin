package com.jdrms.bulletin.domain.home.presentation

import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeTrayMotionTest {
    @Test
    fun progress_follows_upward_drag_and_clamps_in_both_states() {
        assertEquals(0.5f, homeTrayDragProgress(HOME_TRAY_LIFT_DP / 2, false))
        assertEquals(0.5f, homeTrayDragProgress(-HOME_TRAY_LIFT_DP / 2, true))
        assertEquals(0f, homeTrayDragProgress(-1000f, false))
        assertEquals(1f, homeTrayDragProgress(1000f, true))
        assertEquals(0f, homeTrayAlpha(0.3f))
        assertEquals(1f, homeTrayAlpha(1f))
    }

    @Test
    fun receipt_requires_current_listing_token_and_sending_state() {
        val listing = HomeItemId("a")
        val state = HomeInteractionState(
            listingId = listing,
            card = CardState.Sending,
            tray = HomeTrayState(sentReceiptId = 42)
        )
        assertTrue(homeReceiptMatches(42, listing, state))
        assertFalse(homeReceiptMatches(41, listing, state))
        assertFalse(homeReceiptMatches(42, HomeItemId("b"), state))
        assertFalse(homeReceiptMatches(42, listing, state.copy(card = CardState.Leaving)))
        assertEquals(0, homeMotionMillis(HOME_RECEIPT_DWELL_MILLIS, 0f))
        assertEquals(650, homeMotionMillis(HOME_RECEIPT_DWELL_MILLIS, 0.5f))
    }
}
