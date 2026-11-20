package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle

/** Destination geometry for one zoom session; a new [origin] means a new instance, so nothing stale carries over. */
internal class ListingTransitionTargets(
    private val origin: ListingZoomOrigin?,
    private val progress: () -> Float
) {
    private val placements = mutableStateMapOf<ListingTransitionElement, ListingTextPlacement>()

    fun placement(element: ListingTransitionElement): ListingTextPlacement? = placements[element]

    /**
     * Keeps a destination only if the card had this element, and only while the page is at rest. Geometry that
     * appears or changes mid-flight is ignored so the travelling copy never jumps to a newly found target.
     */
    fun record(element: ListingTransitionElement, placement: ListingTextPlacement) {
        if (origin?.texts?.containsKey(element) != true) return
        val p = progress()
        if (p <= 0f || p >= 1f) placements[element] = placement
    }

    fun destinationAlpha(element: ListingTransitionElement): Float =
        listingDestinationAlpha(progress(), origin?.texts?.containsKey(element) == true)
}

/** Provided by [ListingZoomOverlay]; null wherever a detail page is shown without a zoom. */
internal val LocalListingTransitionTargets = compositionLocalOf<ListingTransitionTargets?> { null }

/** Measures a detail text without clipping effects and draws it at the alpha the transition prescribes. */
internal fun Modifier.listingTransitionDestination(
    element: ListingTransitionElement,
    targets: ListingTransitionTargets?,
    probe: ListingTextProbe,
    style: TextStyle,
    color: Color
): Modifier = if (targets == null) {
    this
} else {
    this
        .onGloballyPositioned {
            probe.placed(it, style, color)
            probe.placement()?.let { placement -> targets.record(element, placement) }
        }
        .graphicsLayer { alpha = targets.destinationAlpha(element) }
}
