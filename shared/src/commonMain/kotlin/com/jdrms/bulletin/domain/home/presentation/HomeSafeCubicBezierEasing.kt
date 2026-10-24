package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.animation.core.Easing

/**
 * Inverts the monotone x coordinate without the float analytic solver's endpoint cancellation.
 * After 48 bisections the parameter bracket is at most 2^-48 wide. Double de Casteljau
 * evaluation preserves the original curve, including intentional y overshoot; only endpoints
 * are pinned. For Home's control points, solver error is far below Float rounding error.
 */
internal class HomeSafeCubicBezierEasing(
    private val x1: Float,
    private val y1: Float,
    private val x2: Float,
    private val y2: Float
) : Easing {
    init {
        require(x1 in 0f..1f && x2 in 0f..1f && y1.isFinite() && y2.isFinite())
    }

    override fun transform(fraction: Float): Float {
        if (fraction <= 0f) return 0f
        if (fraction >= 1f) return 1f
        if (fraction.isNaN()) return Float.NaN
        var low = 0.0
        var high = 1.0
        repeat(48) {
            val t = (low + high) * 0.5
            if (coordinate(t, x1.toDouble(), x2.toDouble()) < fraction.toDouble()) low = t else high = t
        }
        return coordinate((low + high) * 0.5, y1.toDouble(), y2.toDouble()).toFloat()
    }

    private fun coordinate(t: Double, first: Double, second: Double): Double {
        val a = t * first
        val b = first + t * (second - first)
        val c = second + t * (1.0 - second)
        val d = a + t * (b - a)
        val e = b + t * (c - b)
        return d + t * (e - d)
    }
}
