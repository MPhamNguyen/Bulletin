package com.jdrms.bulletin.domain.home.presentation

internal data class HomeCardPose(
    val xDp: Float = 0f,
    val yDp: Float = 0f,
    val rotation: Float = 0f,
    val scale: Float = 1f,
    val alpha: Float = 1f,
    val pivotY: Float = 0.9f
)

internal fun homeCardPose(dragDp: Float, kind: HomeDepartureKind?, exit: Float, widthDp: Float): HomeCardPose {
    val rotation = dragDp * 0.05f
    val progress = exit.coerceIn(0f, 1f)
    return when (kind) {
        null -> HomeCardPose(xDp = dragDp, rotation = rotation)
        HomeDepartureKind.PASS -> HomeCardPose(
            xDp = interpolate(dragDp, -1.35f * widthDp, progress),
            rotation = interpolate(rotation, -22f, progress),
            alpha = 1f - progress
        )
        // Profile targeting belongs to 7B. Retain the release position and existing card shape for now.
        HomeDepartureKind.SAVE -> HomeCardPose(
            xDp = dragDp,
            rotation = rotation * (1f - progress),
            scale = interpolate(1f, 0.05f, progress),
            alpha = 1f - progress,
            pivotY = 0.5f
        )
        HomeDepartureKind.SENT -> HomeCardPose(xDp = dragDp, alpha = 1f - progress)
    }
}

internal fun swipeFeedbackProgress(dragDp: Float): Float =
    (kotlin.math.abs(dragDp) / CardGestureThresholds.swipeDistanceDp).coerceIn(0f, 1f)

internal fun interpolate(start: Float, end: Float, progress: Float): Float = start + (end - start) * progress

internal fun homeMotionMillis(normalMillis: Int, durationScale: Float): Int {
    require(durationScale.isFinite() && durationScale >= 0f)
    return (normalMillis * durationScale).toInt()
}
