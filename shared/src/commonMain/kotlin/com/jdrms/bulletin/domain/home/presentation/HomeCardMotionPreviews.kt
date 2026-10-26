package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinFeedCardDefaults
import com.jdrms.bulletin.core.designsystem.BulletinTheme

@Composable
private fun MotionPreview(dark: Boolean, dragDp: Float) {
    BulletinTheme(darkTheme = dark) {
        Box(Modifier.size(390.dp, 500.dp).background(MaterialTheme.colorScheme.background)) {
            Box(
                Modifier.fillMaxSize().padding(8.dp).graphicsLayer {
                    val pose = homeCardPose(dragDp, null, 0f, size.width / density)
                    translationX = pose.xDp.dp.toPx()
                    rotationZ = pose.rotation
                    transformOrigin = TransformOrigin(0.5f, pose.pivotY)
                }.clip(MaterialTheme.shapes.large).background(BulletinFeedCardDefaults.backgroundTop)
            ) {
                Text("Mid-drag", color = BulletinFeedCardDefaults.content, modifier = Modifier.padding(24.dp))
                HomeSwipeFeedback(
                    { if (dragDp > 0f) swipeFeedbackProgress(dragDp) else 0f },
                    { if (dragDp < 0f) swipeFeedbackProgress(dragDp) else 0f }
                )
            }
        }
    }
}

@Preview
@Composable
internal fun SaveMidDragPreview() = MotionPreview(false, 45f)

@Preview
@Composable
internal fun PassMidDragPreview() = MotionPreview(true, -45f)
