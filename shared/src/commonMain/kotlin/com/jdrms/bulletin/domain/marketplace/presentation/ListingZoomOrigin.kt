package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.ui.geometry.Rect

/**
 * Where a listing card sat on screen (root coordinates) when it was tapped, so its page can grow out of it.
 * [texts] and [sourceVisibility] are captured at the same moment and belong to this one session.
 */
internal data class ListingZoomOrigin(
    val listingId: String,
    val card: Rect,
    val photo: Rect,
    val photoUrl: String?,
    val texts: Map<ListingTransitionElement, ListingTextSnapshot> = emptyMap(),
    val sourceVisibility: ListingSourceVisibility? = null
)
