package com.jdrms.bulletin.domain.home.presentation.quickmessage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

internal const val DEFAULT_QUICK_REPLY = "Is this still available?"

internal fun quickMessageReplies(suggestedOffer: String?): List<String> = listOfNotNull(
    DEFAULT_QUICK_REPLY,
    suggestedOffer?.let { "Can you do $it?" },
    "Where can we meet?",
    "Can I see more photos?"
)

@Composable
internal fun QuickReplies(replies: List<String>, onReply: (String) -> Unit) {
    val scroll = rememberScrollState()
    val fadeEdge = scroll.canScrollForward
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                if (fadeEdge) drawTrailingFade()
            }
            .horizontalScroll(scroll)
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        replies.forEach { reply -> ReplyChip(reply, onReply) }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTrailingFade() {
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Black, Color.Transparent),
            startX = size.width - 28.dp.toPx(),
            endX = size.width
        ),
        blendMode = BlendMode.DstIn
    )
}

@Composable
private fun ReplyChip(reply: String, onReply: (String) -> Unit) {
    Surface(
        onClick = { onReply(reply) },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            reply,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1
        )
    }
}
