package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/** Text that travels from a listing card to the same content on its detail page. */
internal enum class ListingTransitionElement { TITLE, PRICE }

/** Where one piece of text sits in root coordinates, and how it is drawn there. */
internal data class ListingTextPlacement(
    val bounds: Rect,
    val baselineY: Float,
    val style: TextStyle,
    val color: Color
)

/** The card side of a matched element: the text displayed at tap time and where it was drawn. */
internal data class ListingTextSnapshot(val text: String, val placement: ListingTextPlacement)
