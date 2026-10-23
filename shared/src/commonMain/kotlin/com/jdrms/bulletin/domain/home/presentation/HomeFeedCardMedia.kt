package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.jdrms.bulletin.core.common.cropSettings
import com.jdrms.bulletin.core.common.imageRequestUrl
import com.jdrms.bulletin.core.designsystem.BulletinFeedCardDefaults
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedItem

@Composable
internal fun BoxScope.HomeFeedCardBackground(
    item: HomeFeedItem,
    photoUrl: String?,
    photoIndex: Int,
    photoCount: Int,
    photoProgress: Float,
    onPreviousPhoto: () -> Unit,
    onNextPhoto: () -> Unit
) {
    HomeFeedArtwork(modifier = Modifier.matchParentSize())
    photoUrl?.let { imageUrl ->
        val crop = imageUrl.cropSettings()
        SubcomposeAsyncImage(
            model = imageUrl.imageRequestUrl(),
            contentDescription = null,
            modifier = Modifier.matchParentSize().scale(1.15f).blur(22.dp),
            contentScale = ContentScale.Crop
        ) { SubcomposeAsyncImageContent() }
        SubcomposeAsyncImage(
            model = imageUrl.imageRequestUrl(),
            contentDescription = "${item.title} listing photo",
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Fit
        ) {
            val intrinsic = painter.intrinsicSize
            SubcomposeAsyncImageContent(
                Modifier.graphicsLayer {
                    val frameW = size.width
                    val frameH = size.height
                    val hasIntrinsic = intrinsic.width.isFinite() && intrinsic.height.isFinite() &&
                        intrinsic.width > 0f && intrinsic.height > 0f
                    val fit = if (hasIntrinsic) minOf(frameW / intrinsic.width, frameH / intrinsic.height) else 1f
                    val fittedW = if (hasIntrinsic) intrinsic.width * fit else frameW
                    val fittedH = if (hasIntrinsic) intrinsic.height * fit else frameH
                    val referenceSide = minOf(frameW, frameH)
                    scaleX = crop.scale
                    scaleY = crop.scale
                    translationX = cropAxisTranslation(crop.offsetX, crop.scale, frameW, referenceSide, fittedW)
                    translationY = cropAxisTranslation(crop.offsetY, crop.scale, frameH, referenceSide, fittedH)
                }
            )
        }
    }
    HomeFeedImageScrim(Modifier.matchParentSize())
    if (photoUrl != null && photoCount > 1) {
        Box(
            modifier = Modifier.align(Alignment.TopStart).fillMaxWidth(0.38f).fillMaxHeight()
                .clickable(onClickLabel = "Previous listing photo", onClick = onPreviousPhoto)
        )
        Box(
            modifier = Modifier.align(Alignment.TopEnd).fillMaxWidth(0.38f).fillMaxHeight()
                .clickable(onClickLabel = "Next listing photo", onClick = onNextPhoto)
        )
        HomeFeedPhotoIndicator(
            photoCount = photoCount,
            selectedIndex = photoIndex,
            activeProgress = photoProgress,
            modifier = Modifier.align(Alignment.TopCenter).padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun HomeFeedPhotoIndicator(
    photoCount: Int,
    selectedIndex: Int,
    activeProgress: Float,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(photoCount) { index ->
            Box(
                modifier = Modifier.weight(1f).height(3.dp)
                    .clip(CircleShape).background(Color.White.copy(alpha = 0.38f))
            ) {
                val fill = when {
                    index < selectedIndex -> 1f
                    index == selectedIndex -> activeProgress
                    else -> 0f
                }
                if (fill > 0f) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(fill).background(Color.White))
                }
            }
        }
    }
}

@Composable
private fun HomeFeedImageScrim(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                0f to Color.Transparent,
                0.46f to Color.Transparent,
                0.56f to BulletinFeedCardDefaults.cardSurface.copy(alpha = 0.16f),
                0.66f to BulletinFeedCardDefaults.cardSurface.copy(alpha = 0.42f),
                0.76f to BulletinFeedCardDefaults.cardSurface.copy(alpha = 0.72f),
                0.88f to BulletinFeedCardDefaults.cardSurface.copy(alpha = 0.94f),
                1f to BulletinFeedCardDefaults.cardSurface
            )
        )
    )
}
