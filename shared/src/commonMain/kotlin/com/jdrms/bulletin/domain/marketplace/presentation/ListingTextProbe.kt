package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.toSize

/**
 * Remembers where one [androidx.compose.material3.Text] was last placed, without recomposing as the screen scrolls.
 *
 * Bounds are unclipped: `boundsInRoot()` would shrink them to any clipping ancestor,
 * including the animated page window.
 */
internal class ListingTextProbe {
    private var coordinates: LayoutCoordinates? = null
    private var baselineOffset = 0f
    private var style = TextStyle.Default
    private var color = Color.Unspecified

    fun placed(coordinates: LayoutCoordinates, style: TextStyle, color: Color) {
        this.coordinates = coordinates
        this.style = style
        this.color = color
    }

    fun laidOut(result: TextLayoutResult) {
        baselineOffset = result.getLineBaseline(0)
    }

    /** Null when the text is not attached to a layout. */
    fun placement(): ListingTextPlacement? {
        val attached = coordinates?.takeIf { it.isAttached } ?: return null
        val bounds = Rect(attached.positionInRoot(), attached.size.toSize())
        return ListingTextPlacement(bounds, bounds.top + baselineOffset, style, color)
    }
}
