package com.jdrms.bulletin.domain.home.presentation

/**
 * Reference gesture thresholds. Distances are dp; velocities are dp per millisecond.
 * Release comparisons are strict: reaching a threshold does not exceed it.
 * Touch slop comes from the caller's platform configuration, not these thresholds.
 */
object CardGestureThresholds {
    /** Monolith `Gesture.SWIPE_X`, in dp. */
    const val swipeDistanceDp: Float = 90f

    /** Monolith `Gesture.SWIPE_VX`, in dp/ms. */
    const val swipeVelocityDpPerMs: Float = 0.6f

    /** Monolith `Gesture.OPEN_DY`, in dp (up positive). */
    const val openDistanceDp: Float = 60f

    /** Monolith `Gesture.OPEN_VY`, in dp/ms (up positive); also used for closing. */
    const val openVelocityDpPerMs: Float = 0.45f

    /** Monolith `Gesture.CLOSE_DY`, in dp; closing compares against its negative. */
    const val closeDistanceDp: Float = 50f
}
