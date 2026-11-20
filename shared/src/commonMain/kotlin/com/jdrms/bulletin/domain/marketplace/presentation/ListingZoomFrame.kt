package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp

/** One instant of the card-to-page zoom. All rectangles are in root coordinates. */
internal data class ListingZoomFrame(
    val progress: Float,
    /** The visible window onto the page: the card's bounds at 0, the whole overlay at 1. */
    val window: Rect,
    /** The travelling photo, or null when there is no card to grow from. */
    val hero: Rect?,
    val heroAlpha: Float,
    val pageAlpha: Float,
    val clipsPage: Boolean
)

/**
 * Choreography for [progress] 0 (card) to 1 (page): the window and photo grow together, the page's info fades in
 * around the photo, and the travelling photo hands over to the page's own photo at the very end.
 */
internal fun listingZoomFrame(
    progress: Float,
    origin: ListingZoomOrigin?,
    overlay: Rect,
    photoTarget: Rect?
): ListingZoomFrame {
    val p = progress.coerceIn(0f, 1f)
    return ListingZoomFrame(
        progress = p,
        window = origin?.let { lerp(it.card, overlay, p) } ?: overlay,
        // Until the page reports where its photo sits, the travelling photo stays on the card.
        hero = origin?.let { o -> photoTarget?.let { lerp(o.photo, it, p) } ?: o.photo },
        heroAlpha = 1f - span(p, HERO_FADE_START, 1f),
        pageAlpha = span(p, PAGE_FADE_START, PAGE_FADE_END),
        clipsPage = origin != null && p < 1f
    )
}

private fun span(value: Float, start: Float, end: Float): Float = ((value - start) / (end - start)).coerceIn(0f, 1f)

private const val PAGE_FADE_START = 0.05f
private const val PAGE_FADE_END = 0.6f
private const val HERO_FADE_START = 0.85f
