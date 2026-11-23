package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ListingTransitionSessionTest {
    private fun placement(top: Float) =
        ListingTextPlacement(Rect(0f, top, 100f, top + 20f), top + 14f, TextStyle.Default, Color.Black)

    private val origin = ListingZoomOrigin(
        listingId = "listing-1",
        card = Rect(0f, 0f, 200f, 300f),
        photo = Rect(0f, 0f, 200f, 200f),
        photoUrl = null,
        texts = mapOf(ListingTransitionElement.TITLE to ListingTextSnapshot("Desk", placement(210f)))
    )

    @Test
    fun `a destination measured before travel starts is kept`() {
        val targets = ListingTransitionTargets(origin) { 0f }

        targets.record(ListingTransitionElement.TITLE, placement(100f))

        assertEquals(placement(100f), targets.placement(ListingTransitionElement.TITLE))
    }

    @Test
    fun `geometry that appears or changes mid flight is ignored`() {
        var progress = 0f
        val targets = ListingTransitionTargets(origin) { progress }
        targets.record(ListingTransitionElement.TITLE, placement(100f))

        progress = 0.5f
        targets.record(ListingTransitionElement.TITLE, placement(400f))

        assertEquals(placement(100f), targets.placement(ListingTransitionElement.TITLE))
    }

    @Test
    fun `a destination first found mid flight is never adopted`() {
        val targets = ListingTransitionTargets(origin) { 0.5f }

        targets.record(ListingTransitionElement.TITLE, placement(100f))

        assertNull(targets.placement(ListingTransitionElement.TITLE))
    }

    @Test
    fun `a settled page may update its destination, for example after scrolling`() {
        var progress = 0f
        val targets = ListingTransitionTargets(origin) { progress }
        targets.record(ListingTransitionElement.TITLE, placement(100f))

        progress = 1f
        targets.record(ListingTransitionElement.TITLE, placement(40f))

        assertEquals(placement(40f), targets.placement(ListingTransitionElement.TITLE))
    }

    @Test
    fun `elements the card did not capture are ignored and never hidden`() {
        val targets = ListingTransitionTargets(origin) { 0f }

        targets.record(ListingTransitionElement.PRICE, placement(100f))

        assertNull(targets.placement(ListingTransitionElement.PRICE))
        assertEquals(1f, targets.destinationAlpha(ListingTransitionElement.PRICE))
        assertEquals(0f, targets.destinationAlpha(ListingTransitionElement.TITLE))
    }

    @Test
    fun `without an origin nothing is recorded or hidden`() {
        val targets = ListingTransitionTargets(null) { 0f }

        targets.record(ListingTransitionElement.TITLE, placement(100f))

        assertNull(targets.placement(ListingTransitionElement.TITLE))
        assertEquals(1f, targets.destinationAlpha(ListingTransitionElement.TITLE))
    }
}
