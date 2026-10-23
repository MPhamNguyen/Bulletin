package com.jdrms.bulletin.domain.home.presentation.quickmessage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun SentConfirmation(
    message: String,
    bubbleProgress: State<Float>,
    receiptProgress: State<Float>
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
    ) {
        val bubbleMaxWidth = maxWidth * 0.8f
        Column(
            modifier = Modifier.align(Alignment.TopEnd),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SentBubble(message, bubbleMaxWidth, bubbleProgress)
            SentReceipt(receiptProgress)
        }
    }
}

@Composable
private fun SentBubble(message: String, maxWidth: androidx.compose.ui.unit.Dp, progress: State<Float>) {
    Surface(
        modifier = Modifier.widthIn(max = maxWidth).graphicsLayer {
            val value = progress.value
            alpha = (value * 3f).coerceIn(0f, 1f)
            scaleX = 0.6f + value * 0.4f
            scaleY = 0.6f + value * 0.4f
            translationX = (1f - value) * 10.dp.toPx()
            translationY = (1f - value) * 52.dp.toPx()
            transformOrigin = TransformOrigin(1f, 1f)
        },
        shape = RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            message,
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun SentReceipt(progress: State<Float>) {
    Row(
        modifier = Modifier.padding(end = 4.dp).graphicsLayer { alpha = progress.value },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DrawnCheckmark(progress)
        Text("Sent", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
    }
}
