package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.util.lerp
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.text.lerp as lerpStyle

/** One instant of a travelling text. Positions are root coordinates; [baselineY] is the first line's baseline. */
internal data class ListingTextFrame(
    val left: Float,
    val baselineY: Float,
    val width: Float,
    val style: TextStyle,
    val color: Color,
    val alpha: Float
)

/**
 * The travelling copy of a card text for overlay [progress] 0 (card) to 1 (page).
 *
 * It arrives at [target] by [TRAVEL_END], then fades out while the real destination text fades in beneath it
 * ([listingDestinationAlpha]). Without a [target] it stays on the card and fades out: no guessing a destination.
 */
internal fun listingTextFrame(
    progress: Float,
    source: ListingTextPlacement,
    target: ListingTextPlacement?
): ListingTextFrame {
    val p = progress.coerceIn(0f, 1f)
    val end = target ?: source
    val travel = if (target == null) 0f else span(p, 0f, TRAVEL_END)
    val fadeStart = if (target == null) MISSING_TARGET_FADE_START else TRAVELLER_FADE_START
    return ListingTextFrame(
        left = lerp(source.bounds.left, end.bounds.left, travel),
        baselineY = lerp(source.baselineY, end.baselineY, travel),
        width = lerp(source.bounds.width, end.bounds.width, travel),
        style = lerpStyle(source.style, end.style, travel),
        color = lerpColor(source.color, end.color, travel),
        alpha = 1f - span(p, fadeStart, 1f)
    )
}

/** Opacity of the real destination text: hidden while its copy travels, then revealed beneath the copy. */
internal fun listingDestinationAlpha(progress: Float, hasSnapshot: Boolean): Float =
    if (hasSnapshot) span(progress.coerceIn(0f, 1f), TRAVEL_END, DESTINATION_REVEAL_END) else 1f

private fun span(value: Float, start: Float, end: Float): Float = ((value - start) / (end - start)).coerceIn(0f, 1f)

// Provisional timings; the prototype gate in the roadmap decides them on a device.
private const val TRAVEL_END = 0.8f
private const val DESTINATION_REVEAL_END = 0.92f
private const val TRAVELLER_FADE_START = 0.88f
private const val MISSING_TARGET_FADE_START = 0.6f
