package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.MotionDurationScale

/** Preserve positive system scales. Missing, negative and non-finite settings use the platform default. */
internal fun homeMotionDurationScale(setting: Float?): Float =
    setting?.takeIf { it.isFinite() && it >= 0f } ?: 1f

@Composable
internal expect fun rememberHomeMotionDurationScale(): Float

/** Durations already include our explicit preference; do not multiply by Android's clock scale again. */
internal object HomeMotionClock : MotionDurationScale {
    override val scaleFactor: Float = 1f
}
