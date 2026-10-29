package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinExtras
import com.jdrms.bulletin.core.designsystem.BulletinSpacing

internal enum class SwipeFeedback { SAVE, PASS }

@Composable
internal fun SwipeStamp(verdict: SwipeFeedback, progress: () -> Float, modifier: Modifier = Modifier) {
    val color = if (verdict == SwipeFeedback.SAVE) BulletinExtras.colors.success else MaterialTheme.colorScheme.error
    val shape = MaterialTheme.shapes.medium
    Row(
        modifier.swipeStampPose(verdict, progress)
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(3.dp, color, shape).padding(horizontal = 14.dp, vertical = BulletinSpacing.s),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (verdict == SwipeFeedback.SAVE) Icons.Outlined.BookmarkBorder else Icons.Rounded.Close,
            contentDescription = null,
            tint = color
        )
        // Home UI currently uses inline labels; no shared string-resource convention exists yet.
        Text(verdict.name, color = color, style = MaterialTheme.typography.titleLarge)
    }
}

private fun Modifier.swipeStampPose(verdict: SwipeFeedback, progress: () -> Float): Modifier = graphicsLayer {
    val value = progress().coerceIn(0f, 1f)
    alpha = (value * 1.4f).coerceIn(0f, 1f)
    rotationZ = if (verdict == SwipeFeedback.SAVE) -12f else 12f
    scaleX = 0.7f + value * 0.3f
    scaleY = scaleX
}
