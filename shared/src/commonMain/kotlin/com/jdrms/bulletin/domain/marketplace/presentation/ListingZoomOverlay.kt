package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.layout.positionInRoot
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
    val targets = remember(origin) { ListingTransitionTargets(origin) { progress.value } }
    // The card's matched texts are hidden (not removed) for exactly as long as their travelling copies exist.
    DisposableEffect(origin) {
        origin?.sourceVisibility?.isHidden = true
        onDispose { origin?.sourceVisibility?.isHidden = false }
    }

    Box(Modifier.fillMaxSize().onGloballyPositioned { overlayBounds = it.boundsInRoot() }) {
        Box(Modifier.fillMaxSize().graphicsLayer { applyPageFrame(frame(), overlayBounds) }) {
            CompositionLocalProvider(LocalListingTransitionTargets provides targets) {
                page(Modifier.onGloballyPositioned { photoTarget = it.squareBoundsInRoot() })
            }
        }
        // Taps during the animation must not reach the page or the grid beneath it.
        if (!isSettled) Box(Modifier.fillMaxSize().blockPointerInput())
        if (origin != null) ListingZoomHero(origin.photoUrl, frame) { overlayBounds.topLeft }
        origin?.texts?.forEach { (element, snapshot) ->
            key(element) {
                ListingTransitionText(
                    text = snapshot.text,
                    frame = {
                        // A target outside the overlay (scrolled away) is treated as missing, so the text fades.
                        val target = targets.placement(element)?.takeIf { overlayBounds.overlaps(it.bounds) }
                        listingTextFrame(progress.value, snapshot.placement, target)
                    },
                    overlayOrigin = { overlayBounds.topLeft }
                )
            }
        }
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
// Unclipped on purpose: boundsInRoot() would let the animated page window change the very target it animates toward.
private fun LayoutCoordinates.squareBoundsInRoot(): Rect {
    val topLeft = positionInRoot()
    return Rect(topLeft.x, topLeft.y, topLeft.x + size.width, topLeft.y + size.width)
}

private fun Modifier.blockPointerInput(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) awaitPointerEvent().changes.forEach { it.consume() }
    }
}
