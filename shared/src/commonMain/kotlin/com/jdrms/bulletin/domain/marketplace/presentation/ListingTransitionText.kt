package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

/**
 * A decorative copy of a card text, drawn above the page and placed by baseline each frame.
 * It is not exposed to accessibility: the destination text is the only semantic copy.
 */
@Composable
internal fun ListingTransitionText(
    text: String,
    frame: () -> ListingTextFrame,
    overlayOrigin: () -> Offset
) {
    val current = frame()
    Text(
        text = text,
        style = current.style,
        color = current.color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clearAndSetSemantics {}
            .layout { measurable, constraints ->
                val placeable = measurable.measure(Constraints(maxWidth = current.width.roundToInt().coerceAtLeast(0)))
                val baseline = placeable[FirstBaseline].takeIf { it != AlignmentLine.Unspecified } ?: 0
                val origin = overlayOrigin()
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.place(
                        (current.left - origin.x).roundToInt(),
                        (current.baselineY - origin.y).roundToInt() - baseline
                    )
                }
            }
            .graphicsLayer { alpha = current.alpha }
    )
}
