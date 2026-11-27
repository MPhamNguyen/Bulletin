package com.jdrms.bulletin.core.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinMotion

private const val DOT_COUNT = 3
private val DotJumpHeight = 4.dp

/**
 * In-progress label whose trailing dots jump up and down in a staggered wave. Each dot keeps its layout
 * slot and only moves via a graphics layer, so the label never changes size or shifts inside a button.
 */
@Composable
fun BulletinLoadingLabel(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium
) {
    val transition = rememberInfiniteTransition(label = "loadingDots")
    val jumpPx = with(LocalDensity.current) { DotJumpHeight.toPx() }
    val offsets = List(DOT_COUNT) { index ->
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = BulletinMotion.SHIMMER
                    0f at 0
                    -jumpPx at BulletinMotion.FAST using FastOutSlowInEasing
                    0f at BulletinMotion.FAST * 2 using FastOutSlowInEasing
                },
                initialStartOffset = StartOffset(index * BulletinMotion.MICRO)
            ),
            label = "loadingDot$index"
        )
    }
    Row(modifier = modifier.clearAndSetSemantics { contentDescription = "$text..." }) {
        Text(text, style = style)
        offsets.forEach { offset -> LoadingDot(offset, style) }
    }
}

@Composable
private fun LoadingDot(offset: State<Float>, style: TextStyle) {
    Text(".", style = style, modifier = Modifier.graphicsLayer { translationY = offset.value })
}
