package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ListingTextTransitionFrameTest {
    private val source = ListingTextPlacement(
        bounds = Rect(32f, 600f, 232f, 620f),
        baselineY = 614f,
        style = TextStyle(fontSize = 14.sp),
        color = Color.Black
    )
    private val target = ListingTextPlacement(
        bounds = Rect(20f, 180f, 300f, 240f),
        baselineY = 210f,
        style = TextStyle(fontSize = 24.sp),
        color = Color.Blue
    )

    @Test
    fun `at the start the copy is exactly where the card drew the text`() {
        val frame = listingTextFrame(0f, source, target)

        assertEquals(source.bounds.left, frame.left)
        assertEquals(source.baselineY, frame.baselineY)
        assertEquals(source.bounds.width, frame.width)
        assertEquals(14.sp, frame.style.fontSize)
        assertEquals(Color.Black, frame.color)
        assertEquals(1f, frame.alpha)
    }

    @Test
    fun `once travel ends the copy sits on the destination baseline in the destination style`() {
        val frame = listingTextFrame(0.8f, source, target)

        assertEquals(target.bounds.left, frame.left)
        assertEquals(target.baselineY, frame.baselineY)
        assertEquals(target.bounds.width, frame.width)
        assertEquals(24.sp, frame.style.fontSize)
        assertEquals(Color.Blue, frame.color)
    }

    @Test
    fun `at the end the copy is gone and the destination is fully shown`() {
        assertEquals(0f, listingTextFrame(1f, source, target).alpha)
        assertEquals(1f, listingDestinationAlpha(1f, hasSnapshot = true))
    }

    @Test
    fun `the copy stays opaque until the destination is fully revealed beneath it`() {
        val handoff = (0..40).map { it / 40f }
        val revealedAt = handoff.first { listingDestinationAlpha(it, hasSnapshot = true) == 1f }

        assertTrue(listingTextFrame(revealedAt, source, target).alpha > 0f)
        assertTrue(handoff.filter { it < 0.8f }.all { listingDestinationAlpha(it, true) == 0f })
    }

    @Test
    fun `progress outside zero to one is clamped`() {
        assertEquals(listingTextFrame(0f, source, target), listingTextFrame(-0.5f, source, target))
        assertEquals(listingTextFrame(1f, source, target), listingTextFrame(1.5f, source, target))
        assertEquals(1f, listingDestinationAlpha(2f, true))
    }

    @Test
    fun `without a destination the copy stays on the card and fades out`() {
        val middle = listingTextFrame(0.5f, source, target = null)
        val end = listingTextFrame(1f, source, target = null)

        assertEquals(source.baselineY, middle.baselineY)
        assertEquals(source.bounds.left, middle.left)
        assertEquals(1f, middle.alpha)
        assertEquals(0f, end.alpha)
    }

    @Test
    fun `text without a card snapshot is never hidden`() {
        assertEquals(1f, listingDestinationAlpha(0f, hasSnapshot = false))
    }
}
