package com.jdrms.bulletin.domain.home.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HomeCardPoseTest {
    @Test
    fun dragRetainsReferenceTiltAndBottomPivot() {
        assertEquals(HomeCardPose(xDp = 45f, rotation = 2.25f), homeCardPose(45f, null, 0f, 360f))
        assertEquals(-2.25f, homeCardPose(-45f, null, 0f, 360f).rotation)
    }

    @Test
    fun passStartsAtReleaseAndLeavesPastLeftEdge() {
        val start = homeCardPose(-100f, HomeDepartureKind.PASS, 0f, 360f)
        val end = homeCardPose(-100f, HomeDepartureKind.PASS, 1f, 360f)
        assertEquals(-100f, start.xDp)
        assertEquals(-486f, end.xDp)
        assertEquals(-22f, end.rotation)
        assertEquals(0f, end.alpha)
    }

    @Test
    fun saveShrinksAndFadesWithoutInventingProfileDestination() {
        val end = homeCardPose(100f, HomeDepartureKind.SAVE, 1f, 360f)
        assertEquals(100f, end.xDp)
        assertEquals(0.05f, end.scale, 0.00001f)
        assertEquals(0f, end.alpha)
        assertEquals(0.5f, end.pivotY)
    }

    @Test
    fun feedbackClampsAtThresholdAndHandlesBothDirections() {
        assertEquals(0f, swipeFeedbackProgress(0f))
        assertEquals(0.5f, swipeFeedbackProgress(-45f))
        assertEquals(1f, swipeFeedbackProgress(180f))
    }

    @Test
    fun oneScaleControlsNormalAndZeroDurations() {
        assertEquals(700, homeMotionMillis(700, 1f))
        assertEquals(0, homeMotionMillis(700, 0f))
        assertEquals(250, homeMotionMillis(500, 0.5f))
        assertFailsWith<IllegalArgumentException> { homeMotionMillis(320, -1f) }
        assertFailsWith<IllegalArgumentException> { homeMotionMillis(320, Float.NaN) }
    }
}
