package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.toSize
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem

/** Remembers where one card and its photo were last placed, without recomposing as the grid scrolls. */
internal class ListingBoundsTracker {
    private var card: LayoutCoordinates? = null
    private var photo: LayoutCoordinates? = null

    fun trackCard(coordinates: LayoutCoordinates) {
        card = coordinates
    }

    fun trackPhoto(coordinates: LayoutCoordinates) {
        photo = coordinates
    }

    /** Null when the card is not on screen, in which case the page simply fades in. */
    fun capture(item: MarketplaceItem): ListingZoomOrigin? {
        val cardBounds = card?.takeIf { it.isAttached }?.unclippedBounds() ?: return null
        val photoBounds = photo?.takeIf { it.isAttached }?.unclippedBounds() ?: return null
        return ListingZoomOrigin(item.id.value, cardBounds, photoBounds, item.photos.firstOrNull())
    }

    // boundsInRoot() would clip a half-scrolled card and squash the photo's square shape.
    private fun LayoutCoordinates.unclippedBounds() = Rect(positionInRoot(), size.toSize())
}
