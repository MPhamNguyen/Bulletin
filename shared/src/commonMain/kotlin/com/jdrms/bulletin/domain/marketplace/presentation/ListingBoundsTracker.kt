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
    private val probes = ListingTransitionElement.entries.associateWith { ListingTextProbe() }

    /** Hides the card's matched texts while their travelling copies are drawn. */
    val sourceVisibility = ListingSourceVisibility()

    fun probe(element: ListingTransitionElement): ListingTextProbe = probes.getValue(element)

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
        val texts = mapOf(
            ListingTransitionElement.TITLE to item.title,
            ListingTransitionElement.PRICE to item.price.formatted
        ).mapNotNull { (element, text) ->
            probe(element).placement()?.let { element to ListingTextSnapshot(text, it) }
        }.toMap()
        return ListingZoomOrigin(
            listingId = item.id.value,
            card = cardBounds,
            photo = photoBounds,
            photoUrl = item.photos.firstOrNull(),
            texts = texts,
            sourceVisibility = sourceVisibility
        )
    }

    // boundsInRoot() would clip a half-scrolled card and squash the photo's square shape.
    private fun LayoutCoordinates.unclippedBounds() = Rect(positionInRoot(), size.toSize())
}
