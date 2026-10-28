package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedItem

private const val HOME_PHOTO_INTERVAL_MILLIS = 3_000L

@Composable
internal fun HomePhotoMedia(
    item: HomeFeedItem,
    cardIndex: Int,
    isActive: Boolean,
    modifier: Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val photoUrls = remember(item.photoUrls, item.imageUrl) {
        item.photoUrls.ifEmpty { listOfNotNull(item.imageUrl) }
    }
    var photoIndex by rememberSaveable(item.id.value, cardIndex, photoUrls) { mutableIntStateOf(0) }
    var isHolding by remember(item.id.value, cardIndex) { mutableStateOf(false) }
    var isLongPress by remember(item.id.value, cardIndex) { mutableStateOf(false) }
    val photoProgress = remember(item.id.value, photoUrls, photoIndex) { Animatable(0f) }
    HomePhotoAdvance(item.id.value, photoUrls, isActive, photoIndex, isHolding, photoProgress) { photoIndex++ }
    Box(modifier.homePhotoHold(item.id.value, cardIndex, { isHolding = it }, { isLongPress = it })) {
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
        content()
    }
}

@Composable
private fun HomePhotoAdvance(
    listingId: String,
    photoUrls: List<String>,
    isActive: Boolean,
    photoIndex: Int,
    isHolding: Boolean,
    photoProgress: Animatable<Float, AnimationVector1D>,
    onNext: () -> Unit
) {
    LaunchedEffect(listingId, photoUrls, isActive, photoIndex, isHolding) {
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
            if (photoIndex < photoUrls.lastIndex) onNext()
        }
    }
}

private fun Modifier.homePhotoHold(
    listingId: String,
    cardIndex: Int,
    onHolding: (Boolean) -> Unit,
    onLongPress: (Boolean) -> Unit
): Modifier = pointerInput(listingId, cardIndex) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        onHolding(true)
        onLongPress(false)
        try {
            val longPress = awaitLongPressOrCancellation(down.id)
            onLongPress(longPress != null)
            if (longPress != null) {
                do {
                    val event = awaitPointerEvent(PointerEventPass.Final)
                } while (event.changes.any { it.pressed })
            }
        } finally {
            onHolding(false)
        }
    }
}
