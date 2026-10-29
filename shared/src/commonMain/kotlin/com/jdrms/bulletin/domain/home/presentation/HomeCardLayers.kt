package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import com.jdrms.bulletin.core.designsystem.BulletinFeedCardDefaults
import com.jdrms.bulletin.core.designsystem.BulletinSpacing

internal fun Modifier.homeCardLayer(
    front: Boolean,
    motion: HomeCardMotion,
    target: (Float) -> HomeSaveTarget?
): Modifier = graphicsLayer {
    if (front) {
        val pose = motion.pose(size.width / density, target(density))
        translationX = pose.xDp * density
        translationY = pose.yDp * density
        rotationZ = pose.rotation
        scaleX = pose.scale
        scaleY = pose.scale
        alpha = pose.alpha
        transformOrigin = TransformOrigin(0.5f, pose.pivotY)
    } else {
        val pose = homeNextCardPose(motion.promotion)
        translationX = 0f
        translationY = 0f
        rotationZ = 0f
        alpha = 1f
        scaleX = pose.scale
        scaleY = pose.scale
        transformOrigin = TransformOrigin(0.5f, 0f)
    }
}

@Composable
internal fun HomeNextCardShade(motion: HomeCardMotion) {
    Box(
        Modifier.fillMaxSize().padding(BulletinSpacing.s).clip(MaterialTheme.shapes.large).drawBehind {
            drawRect(BulletinFeedCardDefaults.scrim.copy(alpha = homeNextCardPose(motion.promotion).shadeAlpha))
        }
    )
}
