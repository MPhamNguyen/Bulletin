package com.jdrms.bulletin.domain.home.presentation

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class HomeSafeCubicBezierEasingTest {
    @Test fun exit() = sweep(0.5f, 0f, 0.85f, 0.45f)
    @Test fun snapBack() = sweep(0.2f, 0.9f, 0.3f, 1.15f)
    @Test fun promotion() = sweep(0.22f, 0.85f, 0.25f, 1f)
    @Test fun tray() = sweep(0.22f, 0.85f, 0.25f, 1f)
    @Test fun pulse() = sweep(0.2f, 0.9f, 0.3f, 1.3f)

    private fun sweep(x1: Float, y1: Float, x2: Float, y2: Float) {
        val easing = HomeSafeCubicBezierEasing(x1, y1, x2, y2)
        val failures = mutableListOf<Float>()
        fun value(fraction: Float): Float = runCatching {
            easing.transform(fraction).also { assertTrue(it.isFinite(), "Non-finite output at $fraction") }
        }.getOrElse {
            failures += fraction
            Float.NaN
        }
        assertEquals(0f, value(0f))
        assertEquals(1f, value(1f))
        assertEquals(0f, value(-1f))
        assertEquals(1f, value(2f))
        val edges = (1..20).flatMap { listOf(Float.fromBits(it), Float.fromBits(1f.toBits() - it)) }
        edges.forEach { value(it) }
        if (failures.isNotEmpty()) fail("Endpoint solver failures at fractions=$failures")
        var previous = 0f
        var maximum = 0f
        for (step in 0..1_000_000) {
            val fraction = step / 1_000_000f
            val actual = value(fraction)
            if (!actual.isNaN()) {
                assertTrue(actual.isFinite())
                if (y2 <= 1f) assertTrue(actual + 0.000001f >= previous)
                if (step % 100 == 0) {
                    val expected = reference(fraction.toDouble(), x1, y1, x2, y2)
                    assertTrue(
                        abs(actual - expected) <= 0.000002,
                        "fraction=$fraction actual=$actual expected=$expected"
                    )
                }
                previous = actual
                maximum = maxOf(maximum, actual)
            }
        }
        if (failures.isNotEmpty()) fail("${failures.size} solver failures; first fractions=${failures.take(25)}")
        if (y2 > 1f) assertTrue(maximum > 1.01f, "Intentional overshoot must survive")
        edges.forEach { fraction ->
            assertTrue(abs(value(fraction) - reference(fraction.toDouble(), x1, y1, x2, y2)) <= 0.000002)
        }
    }

    private fun reference(fraction: Double, x1: Float, y1: Float, x2: Float, y2: Float): Double {
        var low = 0.0
        var high = 1.0
        repeat(80) {
            val t = (low + high) / 2.0
            val x = 3 * (1 - t) * (1 - t) * t * x1 + 3 * (1 - t) * t * t * x2 + t * t * t
            if (x < fraction) low = t else high = t
        }
        val t = (low + high) / 2.0
        return 3 * (1 - t) * (1 - t) * t * y1 + 3 * (1 - t) * t * t * y2 + t * t * t
    }
}
