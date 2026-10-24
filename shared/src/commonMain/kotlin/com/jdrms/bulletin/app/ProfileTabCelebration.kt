package com.jdrms.bulletin.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.domain.home.presentation.homeMotionMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Cosmetic work lives outside the card's exit job, so it cannot delay deck advancement. */
internal class ProfileTabCelebration {
    private val ripple = Animatable(1f)
    private val pulse = Animatable(1f)
    private var job: Job? = null

    fun modifier(color: Color): Modifier = Modifier.drawBehind {
        val progress = ripple.value
        if (progress < 1f) {
            drawCircle(
                color.copy(alpha = 0.9f * (1f - progress)),
                radius = 22.dp.toPx() * (0.4f + 0.8f * progress)
            )
        }
    }.graphicsLayer {
        scaleX = pulse.value
        scaleY = pulse.value
    }

    fun play(scope: CoroutineScope, durationScale: Float, haptics: HapticFeedback) {
        if (durationScale == 0f) return
        job?.cancel()
        job = scope.launch {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            ripple.snapTo(0f)
            pulse.snapTo(1f)
            coroutineScope {
                launch { ripple.animateTo(1f, tween(homeMotionMillis(800, durationScale))) }
                launch {
                    pulse.animateTo(
                        1f,
                        keyframes {
                            durationMillis = homeMotionMillis(420, durationScale)
                            val ease = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1.3f)
                            1.25f at homeMotionMillis(126, durationScale) using ease
                            1f at homeMotionMillis(420, durationScale) using ease
                        }
                    )
                }
            }
        }
    }
}
