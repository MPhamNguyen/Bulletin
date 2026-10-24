package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private val SnapEasing = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1.15f)
private val SettleEasing = CubicBezierEasing(0.22f, 0.85f, 0.25f, 1f)
private val ExitEasing = CubicBezierEasing(0.5f, 0f, 0.85f, 0.45f)

/** UI-local motion. Read its values only from graphics/draw lambdas, not card composition. */
internal class HomeCardMotion(private val durationScale: Float = 1f) {
    var dragDp by mutableFloatStateOf(0f)
        private set
    var exit by mutableFloatStateOf(0f)
        private set
    var promotion by mutableFloatStateOf(0f)
        private set
    private var kind by mutableStateOf<HomeDepartureKind?>(null)
    private var snapJob: Job? = null

    fun begin() {
        snapJob?.cancel()
        dragDp = 0f
        promotion = 0f
    }

    fun drag(valueDp: Float) {
        dragDp = valueDp
        promotion = homeDragPromotion(valueDp)
    }

    fun pose(widthDp: Float): HomeCardPose = homeCardPose(dragDp, kind, exit, widthDp)

    fun saveProgress(): Float = if (kind == HomeDepartureKind.SAVE) {
        1f
    } else {
        if (dragDp > 0f) swipeFeedbackProgress(dragDp) else 0f
    }

    fun passProgress(): Float = if (kind == HomeDepartureKind.PASS) {
        1f
    } else {
        if (dragDp < 0f) swipeFeedbackProgress(dragDp) else 0f
    }

    fun snapBack(scope: CoroutineScope) {
        snapJob?.cancel()
        snapJob = scope.launch {
            animate(dragDp, 0f, animationSpec = tween(homeMotionMillis(320, durationScale), easing = SnapEasing)) {
                    value, _ ->
                drag(value)
            }
        }
    }

    suspend fun leave(departure: HomeDeparture) {
        snapJob?.cancel()
        kind = departure.kind
        val duration = when (departure.kind) {
            HomeDepartureKind.PASS -> 500
            HomeDepartureKind.SAVE -> 700
            HomeDepartureKind.SENT -> 600
        }
        val promotionFrom = promotion
        val millis = homeMotionMillis(duration, durationScale)
        coroutineScope {
            launch {
                animate(0f, 1f, animationSpec = tween(millis, easing = ExitEasing)) { value, _ ->
                    exit = value
                }
            }
            launch {
                animate(promotionFrom, 1f, animationSpec = tween(millis, easing = SettleEasing)) { value, _ ->
                    promotion = value
                }
            }
        }
    }
}
