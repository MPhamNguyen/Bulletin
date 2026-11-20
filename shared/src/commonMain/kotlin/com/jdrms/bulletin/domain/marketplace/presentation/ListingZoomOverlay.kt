package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.jdrms.bulletin.core.designsystem.theme.BulletinMotion
import com.jdrms.bulletin.core.designsystem.theme.BulletinShapes

/**
 * Grows [page] out of the card it was opened from and shrinks it back when [isOpen] turns false.
 *
 * The page stays composed until the close animation ends, so callers must keep supplying the last open content.
 * [page] receives a modifier that marks where its hero photo sits, which is where the travelling photo lands.
 * Without an [origin] the page just fades in and out.
 */
@Composable
internal fun ListingZoomOverlay(
    isOpen: Boolean,
    origin: ListingZoomOrigin?,
    page: @Composable (photoModifier: Modifier) -> Unit
) {
    val progress = remember { Animatable(0f) }
    var isShown by remember { mutableStateOf(isOpen) }
    LaunchedEffect(isOpen) {
        if (isOpen) {
            isShown = true
            progress.animateTo(1f, tween(BulletinMotion.EMPHASIZED, easing = FastOutSlowInEasing))
        } else {
            progress.animateTo(0f, tween(BulletinMotion.STANDARD, easing = FastOutSlowInEasing))
            isShown = false
        }
    }
    if (!isShown) return

    var overlayBounds by remember { mutableStateOf(Rect.Zero) }
    var photoTarget by remember { mutableStateOf<Rect?>(null) }
    val isSettled by remember { derivedStateOf { progress.value >= 1f } }
    val frame = { listingZoomFrame(progress.value, origin, overlayBounds, photoTarget) }

    Box(Modifier.fillMaxSize().onGloballyPositioned { overlayBounds = it.boundsInRoot() }) {
        Box(Modifier.fillMaxSize().graphicsLayer { applyPageFrame(frame(), overlayBounds) }) {
            page(Modifier.onGloballyPositioned { photoTarget = it.squareBoundsInRoot() })
        }
        // Taps during the animation must not reach the page or the grid beneath it.
        if (!isSettled) Box(Modifier.fillMaxSize().blockPointerInput())
        if (origin != null) ListingZoomHero(origin.photoUrl, frame) { overlayBounds.topLeft }
    }
}

private fun GraphicsLayerScope.applyPageFrame(frame: ListingZoomFrame, overlay: Rect) {
    alpha = frame.pageAlpha
    if (frame.clipsPage) {
        clip = true
        shape = ListingZoomWindowShape(
            bounds = frame.window.translate(-overlay.topLeft),
            corners = BulletinShapes.listingZoomWindow(frame.progress)
        )
    }
}

// The photo slot also holds the pager's page dots once a listing has several photos; only the square is the photo.
private fun LayoutCoordinates.squareBoundsInRoot(): Rect {
    val bounds = boundsInRoot()
    return Rect(bounds.left, bounds.top, bounds.right, bounds.top + bounds.width)
}

private fun Modifier.blockPointerInput(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) awaitPointerEvent().changes.forEach { it.consume() }
    }
}
