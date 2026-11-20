package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/** Clips a layer to [bounds] (in the layer's own coordinates), using [corners] sized to those bounds. */
internal class ListingZoomWindowShape(
    private val bounds: Rect,
    private val corners: Shape
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            addOutline(corners.createOutline(bounds.size, layoutDirection, density))
            translate(bounds.topLeft)
        }
        return Outline.Generic(path)
    }
}
