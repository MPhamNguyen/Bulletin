package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinFeedCardDefaults

@Composable
internal fun HomePassTint(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier.background(
            Brush.horizontalGradient(
                listOf(
                    BulletinFeedCardDefaults.passTint.copy(alpha = progress * 0.5f),
                    BulletinFeedCardDefaults.passTint.copy(alpha = 0f)
                )
            )
        )
    )
}

@Composable
internal fun HomePassStamp(progress: Float, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(top = 30.dp, end = 20.dp).rotate(12f).graphicsLayer {
            alpha = (progress * 1.4f).coerceIn(0f, 1f)
            scaleX = 0.7f + progress * 0.3f
            scaleY = scaleX
        },
        shape = MaterialTheme.shapes.medium,
        color = BulletinFeedCardDefaults.cardSurface.copy(alpha = 0.4f),
        contentColor = BulletinFeedCardDefaults.passStamp,
        border = BorderStroke(3.dp, BulletinFeedCardDefaults.passStamp)
    ) {
        Text(
            "× PASS",
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleLarge
        )
    }
}
