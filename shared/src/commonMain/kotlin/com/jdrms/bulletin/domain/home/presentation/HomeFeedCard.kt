package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.jdrms.bulletin.core.common.cropSettings
import com.jdrms.bulletin.core.common.imageRequestUrl
import com.jdrms.bulletin.core.designsystem.theme.BulletinFeedCardDefaults
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedItem
import kotlin.math.max
import kotlin.math.min

@Composable
fun HomeFeedCard(
    item: HomeFeedItem,
    modifier: Modifier = Modifier,
    cardIndex: Int = 0,
    isActive: Boolean = true,
    mediaModifier: Modifier = Modifier,
    informationBottomPadding: Dp = 0.dp
) {
    var descriptionExpanded by rememberSaveable(item.id.value, cardIndex) { mutableStateOf(false) }
    HomePhotoMedia(
        item,
        cardIndex,
        isActive,
        modifier.fillMaxSize().padding(BulletinSpacing.s)
            .clip(MaterialTheme.shapes.large).then(mediaModifier)
    ) {
        HomeFeedInformation(
            item = item,
            expanded = descriptionExpanded,
            onToggle = { descriptionExpanded = !descriptionExpanded },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = informationBottomPadding)
        )
    }
}

internal fun stepHomePhotoIndex(currentIndex: Int, direction: Int, photoCount: Int): Int {
    if (photoCount <= 0) return 0
    return (currentIndex + direction).coerceIn(0, photoCount - 1)
}

internal fun cropAxisTranslation(
    offset: Float,
    scale: Float,
    frameExtent: Float,
    referenceSide: Float,
    fittedExtent: Float
): Float {
    val raw = -offset * referenceSide * scale
    val maxTranslation = max(0f, (fittedExtent * scale - frameExtent) / 2f)
    return raw.coerceIn(-maxTranslation, maxTranslation)
}

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
    HomeFeedPhotos(photoUrl, item.title)
    HomeFeedImageScrim(Modifier.matchParentSize())
    if (photoUrl != null && photoCount > 1) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(0.38f)
                .fillMaxHeight()
                .clickable(onClickLabel = "Previous listing photo", onClick = onPreviousPhoto)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxWidth(0.38f)
                .fillMaxHeight()
                .clickable(onClickLabel = "Next listing photo", onClick = onNextPhoto)
        )
        HomeFeedPhotoIndicator(
            photoCount = photoCount,
            selectedIndex = photoIndex,
            activeProgress = photoProgress,
            modifier = Modifier.align(Alignment.TopCenter).padding(horizontal = BulletinSpacing.m, vertical = 10.dp)
        )
    }
}

@Composable
private fun BoxScope.HomeFeedPhotos(photoUrl: String?, title: String) {
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
            contentDescription = "$title listing photo",
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
                    val fit = if (hasIntrinsic) min(frameW / intrinsic.width, frameH / intrinsic.height) else 1f
                    val fittedW = if (hasIntrinsic) intrinsic.width * fit else frameW
                    val fittedH = if (hasIntrinsic) intrinsic.height * fit else frameH
                    val referenceSide = min(frameW, frameH)

                    scaleX = crop.scale
                    scaleY = crop.scale
                    translationX = cropAxisTranslation(crop.offsetX, crop.scale, frameW, referenceSide, fittedW)
                    translationY = cropAxisTranslation(crop.offsetY, crop.scale, frameH, referenceSide, fittedH)
                }
            )
        }
    }
}

@Composable
private fun HomeFeedPhotoIndicator(
    photoCount: Int,
    selectedIndex: Int,
    activeProgress: Float,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.xs)) {
        repeat(photoCount) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.38f))
            ) {
                val fill = when {
                    index < selectedIndex -> 1f
                    index == selectedIndex -> activeProgress
                    else -> 0f
                }
                if (fill > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fill)
                            .background(Color.White)
                    )
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
                0f to MaterialTheme.colorScheme.scrim.copy(alpha = 0.28f),
                0.35f to Color.Transparent,
                1f to BulletinFeedCardDefaults.scrim.copy(alpha = 0.92f)
            )
        )
    )
}
