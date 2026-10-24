package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.jdrms.bulletin.core.common.cropSettings
import com.jdrms.bulletin.core.common.imageRequestUrl
import com.jdrms.bulletin.core.designsystem.BulletinFeedCardDefaults
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedItem
import kotlin.math.max
import kotlin.math.min

private const val HOME_PHOTO_INTERVAL_MILLIS = 3_000L

@Composable
fun HomeFeedCard(
    item: HomeFeedItem,
    modifier: Modifier = Modifier,
    cardIndex: Int = 0,
    isActive: Boolean = true
) {
    var descriptionExpanded by rememberSaveable(item.id.value, cardIndex) { mutableStateOf(false) }
    val photoUrls = remember(item.photoUrls, item.imageUrl) {
        item.photoUrls.ifEmpty { listOfNotNull(item.imageUrl) }
    }
    var photoIndex by rememberSaveable(item.id.value, cardIndex, photoUrls) { mutableIntStateOf(0) }
    var isHolding by remember(item.id.value, cardIndex) { mutableStateOf(false) }
    var isLongPress by remember(item.id.value, cardIndex) { mutableStateOf(false) }
    val photoProgress = remember(item.id.value, photoUrls, photoIndex) { Animatable(0f) }
    LaunchedEffect(item.id.value, photoUrls, isActive, photoIndex, isHolding) {
        if (isActive && !isHolding && photoUrls.size > 1) {
            photoProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = (HOME_PHOTO_INTERVAL_MILLIS * (1f - photoProgress.value))
                        .toInt()
                        .coerceAtLeast(1),
                    easing = LinearEasing
                )
            )
            if (photoIndex < photoUrls.lastIndex) photoIndex++
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
            .clip(MaterialTheme.shapes.large)
            .pointerInput(item.id.value, cardIndex) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isHolding = true
                    isLongPress = false
                    try {
                        val longPress = awaitLongPressOrCancellation(down.id)
                        isLongPress = longPress != null
                        if (longPress != null) {
                            do {
                                val event = awaitPointerEvent(PointerEventPass.Final)
                            } while (event.changes.any { it.pressed })
                        }
                    } finally {
                        isHolding = false
                    }
                }
            }
    ) {
        HomeFeedCardBackground(
            item = item,
            photoUrl = photoUrls.getOrNull(photoIndex),
            photoIndex = photoIndex,
            photoCount = photoUrls.size,
            photoProgress = photoProgress.value,
            onPreviousPhoto = {
                if (!isLongPress) photoIndex = stepHomePhotoIndex(photoIndex, -1, photoUrls.size)
            },
            onNextPhoto = {
                if (!isLongPress) photoIndex = stepHomePhotoIndex(photoIndex, 1, photoUrls.size)
            }
        )
        HomeFeedInformation(
            item = item,
            expanded = descriptionExpanded,
            onToggle = { descriptionExpanded = !descriptionExpanded },
            modifier = Modifier.align(Alignment.BottomCenter)
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
private fun BoxScope.HomeFeedCardBackground(
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
