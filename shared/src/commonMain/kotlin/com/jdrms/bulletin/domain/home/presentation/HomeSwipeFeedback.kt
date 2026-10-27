package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinExtras

@Composable
internal fun HomeSwipeFeedback(save: () -> Float, pass: () -> Float, modifier: Modifier = Modifier) {
    val saveColor = BulletinExtras.colors.success
    val passColor = MaterialTheme.colorScheme.error
    Box(modifier.fillMaxSize().clip(MaterialTheme.shapes.large)) {
        Box(
            Modifier.fillMaxSize().drawBehind {
                drawRect(
                    Brush.horizontalGradient(
                        0f to saveColor.copy(alpha = 0.55f * save()),
                        0.65f to saveColor.copy(alpha = 0f),
                        startX = size.width,
                        endX = 0f
                    )
                )
                drawRect(
                    Brush.horizontalGradient(
                        0f to passColor.copy(alpha = 0.5f * pass()),
                        0.65f to passColor.copy(alpha = 0f)
                    )
                )
            }
        )
        SwipeStamp(SwipeFeedback.SAVE, save, Modifier.align(Alignment.TopStart).padding(start = 20.dp, top = 30.dp))
        SwipeStamp(SwipeFeedback.PASS, pass, Modifier.align(Alignment.TopEnd).padding(end = 20.dp, top = 30.dp))
    }
}
