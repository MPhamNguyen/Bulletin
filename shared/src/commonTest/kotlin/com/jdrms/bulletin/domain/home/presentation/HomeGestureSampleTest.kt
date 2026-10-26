package com.jdrms.bulletin.domain.home.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HomeGestureSampleTest {
    @Test
    fun convertsPixelsAndPerSecondVelocityWithUpPositive() {
        assertEquals(HomeGestureSample(90f, 60f, 0.6f, 0.45f), homeGestureSample(180f, -120f, 1200f, -900f, 2f))
        assertEquals(HomeGestureSample(-90f, -60f, -0.6f, -0.45f), homeGestureSample(-270f, 180f, -1800f, 1350f, 3f))
    }

    @Test
    fun rejectsInvalidDensity() {
        listOf(0f, -1f, Float.NaN).forEach { density ->
            assertFailsWith<IllegalArgumentException> { homeGestureSample(0f, 0f, 0f, 0f, density) }
        }
    }

    @Test
    fun mapsEveryReleaseToOnlyAnExplicitLogicalCommand() {
        val expected = listOf(
            HomeReleaseCommand.SAVE,
            HomeReleaseCommand.PASS,
            HomeReleaseCommand.CANCEL,
            HomeReleaseCommand.OPEN,
            HomeReleaseCommand.CLOSE,
            HomeReleaseCommand.OPEN
        )
        CardGestureResolver.Release.entries.zip(expected).forEach { (release, command) ->
            assertEquals(command, homeReleaseCommand(release, saveAvailable = true))
        }
    }

    @Test
    fun unavailableSaveCancelsWhileGuestPassStillCommits() {
        assertEquals(HomeReleaseCommand.CANCEL, homeReleaseCommand(CardGestureResolver.Release.SAVE, false))
        assertEquals(HomeReleaseCommand.PASS, homeReleaseCommand(CardGestureResolver.Release.PASS, false))
    }
}
