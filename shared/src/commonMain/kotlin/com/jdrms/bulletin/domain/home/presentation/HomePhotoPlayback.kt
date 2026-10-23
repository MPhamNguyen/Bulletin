package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedItem
import kotlin.math.max

private const val HOME_PHOTO_INTERVAL_MILLIS = 3_000L

internal class HomePhotoPlayback(val urls: List<String>, private val selectedIndex: MutableState<Int>) {
    var index: Int by selectedIndex
    var isHolding by mutableStateOf(false)
    var isLongPress by mutableStateOf(false)
    var progress: Animatable<Float, AnimationVector1D> = Animatable(0f)

    fun step(direction: Int) {
        if (!isLongPress) index = stepHomePhotoIndex(index, direction, urls.size)
    }
}

@Composable
internal fun rememberHomePhotoPlayback(item: HomeFeedItem, active: Boolean, trayOpen: Boolean): HomePhotoPlayback {
    val urls = remember(item.photoUrls, item.imageUrl) { item.photoUrls.ifEmpty { listOfNotNull(item.imageUrl) } }
    val index = rememberSaveable(item.id.value, urls) { mutableStateOf(0) }
    val photos = remember(item.id, urls) { HomePhotoPlayback(urls, index) }
    photos.progress = remember(item.id, urls, photos.index) { Animatable(0f) }
    val canPlay = active && !photos.isHolding && !trayOpen
    LaunchedEffect(photos, photos.index, canPlay) {
        if (canPlay && urls.size > 1) {
            photos.progress.animateTo(
                1f,
                tween(
                    (HOME_PHOTO_INTERVAL_MILLIS * (1f - photos.progress.value)).toInt().coerceAtLeast(1),
                    easing = LinearEasing
                )
            )
            if (photos.index < urls.lastIndex) photos.index++
        }
    }
    return photos
}

internal fun Modifier.homePhotoHold(photos: HomePhotoPlayback): Modifier = pointerInput(photos) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        photos.isHolding = true
        photos.isLongPress = false
        try {
            val longPress = awaitLongPressOrCancellation(down.id)
            photos.isLongPress = longPress != null
            if (longPress != null) {
                do {
                    val event = awaitPointerEvent(PointerEventPass.Final)
                } while (event.changes.any { it.pressed })
            }
        } finally {
            photos.isHolding = false
        }
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
