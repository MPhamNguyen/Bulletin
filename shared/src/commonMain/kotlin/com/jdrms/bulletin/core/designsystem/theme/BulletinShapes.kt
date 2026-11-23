package com.jdrms.bulletin.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp

/** Named shapes for surfaces with a specific role beside Material's scale. */
object BulletinShapes {
    // Float constants are inlined at compile time, so they are safe to use in property initializers below.
    private const val LISTING_CARD_RADIUS_DP = 22f
    private const val DETAIL_PHOTO_RADIUS_DP = 12f // Matches the Material `medium` shape the detail photo uses.

    val pill: Shape = CircleShape
    val card = RoundedCornerShape(24.dp)
    val cardCompact = RoundedCornerShape(20.dp)
    val listingCard = RoundedCornerShape(LISTING_CARD_RADIUS_DP.dp)
    val searchField = RoundedCornerShape(18.dp)
    val dialog = RoundedCornerShape(28.dp)
    val inset = RoundedCornerShape(14.dp)
    val sheet = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    val sheetLarge = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val segment = RoundedCornerShape(2.dp)
    val swatch = RoundedCornerShape(1.dp)

    /** Corners of the listing page as it grows from its card (0f) to the full screen (1f). */
    fun listingZoomWindow(fraction: Float): Shape =
        RoundedCornerShape(lerp(LISTING_CARD_RADIUS_DP.dp, 0.dp, fraction.coerceIn(0f, 1f)))

    /** Corners of a listing photo as it travels from its card (0f) to the detail page (1f). */
    fun listingZoomPhoto(fraction: Float): Shape =
        RoundedCornerShape(lerp(LISTING_CARD_RADIUS_DP.dp, DETAIL_PHOTO_RADIUS_DP.dp, fraction.coerceIn(0f, 1f)))

    /** The final bubble in a group has a tighter corner toward its sender. */
    fun bubble(squareBottomStart: Boolean, squareBottomEnd: Boolean): CornerBasedShape = RoundedCornerShape(
        topStart = BUBBLE_RADIUS,
        topEnd = BUBBLE_RADIUS,
        bottomEnd = if (squareBottomEnd) BUBBLE_TAIL_RADIUS else BUBBLE_RADIUS,
        bottomStart = if (squareBottomStart) BUBBLE_TAIL_RADIUS else BUBBLE_RADIUS
    )

    private val BUBBLE_RADIUS = 18.dp
    private val BUBBLE_TAIL_RADIUS = 4.dp
}

/** Material's scale; use BulletinShapes.pill for fully rounded ends. */
internal val BulletinMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = BulletinShapes.dialog
)
