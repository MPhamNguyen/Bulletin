package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ListingZoomFrameTest {
    private val overlay = Rect(0f, 100f, 400f, 900f)
    private val origin = ListingZoomOrigin(
        listingId = "listing-1",
        card = Rect(20f, 300f, 220f, 560f),
        photo = Rect(20f, 300f, 220f, 500f),
        photoUrl = "https://example.test/photo.jpg"
    )
    private val photoTarget = Rect(20f, 160f, 380f, 520f)

    @Test
    fun `at the start the window is the card and the photo is where it was tapped`() {
        val frame = listingZoomFrame(0f, origin, overlay, photoTarget)

        assertEquals(origin.card, frame.window)
        assertEquals(origin.photo, frame.hero)
        assertEquals(1f, frame.heroAlpha)
        assertEquals(0f, frame.pageAlpha)
        assertTrue(frame.clipsPage)
    }

    @Test
    fun `at the end the window is the whole overlay and the page owns its photo`() {
        val frame = listingZoomFrame(1f, origin, overlay, photoTarget)

        assertEquals(overlay, frame.window)
        assertEquals(photoTarget, frame.hero)
        assertEquals(0f, frame.heroAlpha)
        assertEquals(1f, frame.pageAlpha)
        assertFalse(frame.clipsPage)
    }

    @Test
    fun `halfway the photo is midway between card and page photo`() {
        val frame = listingZoomFrame(0.5f, origin, overlay, photoTarget)

        assertEquals(Rect(20f, 230f, 300f, 510f), frame.hero)
        assertEquals(1f, frame.heroAlpha)
    }

    @Test
    fun `the page fades in before the travelling photo fades out`() {
        val frames = (0..20).map { listingZoomFrame(it / 20f, origin, overlay, photoTarget) }

        assertTrue(frames.zipWithNext().all { (a, b) -> b.pageAlpha >= a.pageAlpha })
        assertTrue(frames.zipWithNext().all { (a, b) -> b.heroAlpha <= a.heroAlpha })
        assertTrue(frames.first { it.heroAlpha < 1f }.pageAlpha == 1f)
    }

    @Test
    fun `progress outside zero to one is clamped`() {
        val start = listingZoomFrame(0f, origin, overlay, photoTarget)
        val end = listingZoomFrame(1f, origin, overlay, photoTarget)

        assertEquals(start, listingZoomFrame(-0.3f, origin, overlay, photoTarget))
        assertEquals(end, listingZoomFrame(1.4f, origin, overlay, photoTarget))
    }

    @Test
    fun `without an origin the page only fades and there is no travelling photo`() {
        val frame = listingZoomFrame(0.3f, null, overlay, photoTarget)

        assertNull(frame.hero)
        assertEquals(overlay, frame.window)
        assertFalse(frame.clipsPage)
        assertTrue(frame.pageAlpha > 0f && frame.pageAlpha < 1f)
    }

    @Test
    fun `the travelling photo stays on the card until the page reports where its photo is`() {
        assertEquals(origin.photo, listingZoomFrame(0.5f, origin, overlay, photoTarget = null).hero)
        assertNotEquals(origin.photo, listingZoomFrame(0.5f, origin, overlay, photoTarget).hero)
    }
}
