package com.jdrms.bulletin.domain.home.presentation

import kotlin.math.abs
import kotlin.math.hypot

/** Pure gesture decisions. Vertical displacement and velocity are positive when the finger moves UP. */
object CardGestureResolver {
    enum class Axis { X, Y }

    enum class Release { SAVE, PASS, CANCEL, OPEN, CLOSE, STAY_OPEN }

    /** Call until an axis locks; the caller retains that axis for the rest of the gesture. */
    fun resolveAxis(dxDp: Float, dyDp: Float, touchSlopDp: Float, isOpen: Boolean): Axis? = when {
        hypot(dxDp, dyDp) < touchSlopDp -> null
        !isOpen && abs(dxDp) > abs(dyDp) -> Axis.X
        else -> Axis.Y
    }

    /** Distances are dp and velocities are dp/ms; dyDp and vyDpPerMs are up positive. */
    fun resolveRelease(
        axis: Axis?,
        dxDp: Float,
        dyDp: Float,
        vxDpPerMs: Float,
        vyDpPerMs: Float,
        isOpen: Boolean,
        tapOnMedia: Boolean,
    ): Release = when (axis) {
        null -> when {
            !isOpen -> Release.CANCEL
            tapOnMedia -> Release.CLOSE
            else -> Release.STAY_OPEN
        }
        Axis.X -> if (isOpen) Release.STAY_OPEN else resolveSwipe(dxDp, vxDpPerMs)
        Axis.Y -> if (isOpen) {
            if (dyDp < -CardGestureThresholds.closeDistanceDp ||
                vyDpPerMs < -CardGestureThresholds.openVelocityDpPerMs
            ) {
                Release.CLOSE
            } else {
                Release.STAY_OPEN
            }
        } else {
            if (dyDp > CardGestureThresholds.openDistanceDp ||
                vyDpPerMs > CardGestureThresholds.openVelocityDpPerMs
            ) {
                Release.OPEN
            } else {
                Release.CANCEL
            }
        }
    }

    private fun resolveSwipe(dxDp: Float, vxDpPerMs: Float): Release = when {
        dxDp > CardGestureThresholds.swipeDistanceDp -> Release.SAVE
        dxDp < -CardGestureThresholds.swipeDistanceDp -> Release.PASS
        dxDp > 0f && vxDpPerMs < 0f || dxDp < 0f && vxDpPerMs > 0f -> Release.CANCEL
        vxDpPerMs > CardGestureThresholds.swipeVelocityDpPerMs -> Release.SAVE
        vxDpPerMs < -CardGestureThresholds.swipeVelocityDpPerMs -> Release.PASS
        else -> Release.CANCEL
    }
}
