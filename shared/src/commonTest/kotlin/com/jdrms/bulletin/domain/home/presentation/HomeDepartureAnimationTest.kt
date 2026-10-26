package com.jdrms.bulletin.domain.home.presentation

import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeDepartureAnimationTest {
    private val listing = HomeItemId("current")
    private val departure = HomeDeparture(1, listing, HomeDepartureKind.PASS)
    private val leaving = HomeInteractionState(listingId = listing, card = CardState.Leaving, departure = departure)

    @Test
    fun onlyCurrentLeavingTokenCanBeAcknowledged() {
        assertTrue(homeDepartureMatches(departure, leaving, listing))
        assertFalse(homeDepartureMatches(departure.copy(id = 2), leaving, listing))
        assertFalse(homeDepartureMatches(departure, leaving, HomeItemId("next")))
        assertFalse(homeDepartureMatches(departure, leaving.copy(card = CardState.Idle), listing))
        assertFalse(homeDepartureMatches(departure, leaving.copy(departure = null), listing))
    }
}
