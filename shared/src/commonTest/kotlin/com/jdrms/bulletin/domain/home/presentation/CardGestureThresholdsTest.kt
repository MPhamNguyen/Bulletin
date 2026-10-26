package com.jdrms.bulletin.domain.home.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class CardGestureThresholdsTest {
    @Test
    fun reference_distances_are_dp_and_velocities_are_dp_per_millisecond() {
        assertEquals(90f, CardGestureThresholds.swipeDistanceDp)
        assertEquals(0.6f, CardGestureThresholds.swipeVelocityDpPerMs)
        assertEquals(60f, CardGestureThresholds.openDistanceDp)
        assertEquals(0.45f, CardGestureThresholds.openVelocityDpPerMs)
        assertEquals(50f, CardGestureThresholds.closeDistanceDp)
    }
}
