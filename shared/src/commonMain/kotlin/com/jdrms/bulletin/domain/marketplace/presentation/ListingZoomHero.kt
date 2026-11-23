package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import com.jdrms.bulletin.core.designsystem.theme.BulletinShapes
import kotlin.math.roundToInt

/** The card's photo, drawn above the page and moved and resized each frame from [frame] without recomposing. */
@Composable
internal fun ListingZoomHero(
    photoUrl: String?,
    frame: () -> ListingZoomFrame,
    overlayOrigin: () -> Offset
) {
    MarketplaceListingPhotoImage(
        photoUrl = photoUrl,
        contentDescription = null,
        modifier = Modifier
            .layout { measurable, constraints ->
                val bounds = frame().hero?.translate(-overlayOrigin())
                val width = bounds?.width?.roundToInt()?.coerceAtLeast(0) ?: 0
                val height = bounds?.height?.roundToInt()?.coerceAtLeast(0) ?: 0
                val placeable = measurable.measure(Constraints.fixed(width, height))
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.place(bounds?.topLeft?.round() ?: IntOffset.Zero)
                }
            }
            .graphicsLayer {
                val current = frame()
                alpha = if (current.hero == null) 0f else current.heroAlpha
                shape = BulletinShapes.listingZoomPhoto(current.progress)
                clip = true
            }
    )
}
