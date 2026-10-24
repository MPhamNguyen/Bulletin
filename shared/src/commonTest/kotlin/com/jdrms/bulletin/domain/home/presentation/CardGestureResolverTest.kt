package com.jdrms.bulletin.domain.home.presentation

import com.jdrms.bulletin.domain.home.presentation.CardGestureResolver.Axis
import com.jdrms.bulletin.domain.home.presentation.CardGestureResolver.Release
import kotlin.test.Test
import kotlin.test.assertEquals

class CardGestureResolverTest {
    @Test
    fun horizontal_distance_boundaries_are_strict() {
        listOf(
            89.99f to Release.CANCEL, 90f to Release.CANCEL, 90.01f to Release.SAVE,
            -89.99f to Release.CANCEL, -90f to Release.CANCEL, -90.01f to Release.PASS,
        ).forEach { (dx, expected) ->
            assertEquals(expected, release(Axis.X, dxDp = dx), "dx=$dx")
        }
    }

    @Test
    fun horizontal_velocity_boundaries_are_strict_with_zero_or_agreeing_displacement() {
        listOf(
            0.5999f to Release.CANCEL, 0.6f to Release.CANCEL, 0.6001f to Release.SAVE,
            -0.5999f to Release.CANCEL, -0.6f to Release.CANCEL, -0.6001f to Release.PASS,
        ).forEach { (vx, expected) ->
            listOf(0f, vx).forEach { dx ->
                assertEquals(expected, release(Axis.X, dxDp = dx, vxDpPerMs = vx), "dx=$dx vx=$vx")
            }
        }
    }

    @Test
    fun displacement_wins_over_opposing_velocity_only_past_the_distance_threshold() {
        listOf(
            Triple(90.01f, -10f, Release.SAVE), Triple(-90.01f, 10f, Release.PASS),
            Triple(90f, -10f, Release.CANCEL), Triple(-90f, 10f, Release.CANCEL),
            Triple(1f, -10f, Release.CANCEL), Triple(-1f, 10f, Release.CANCEL),
        ).forEach { (dx, vx, expected) ->
            assertEquals(expected, release(Axis.X, dxDp = dx, vxDpPerMs = vx), "dx=$dx vx=$vx")
        }
    }

    @Test
    fun closed_vertical_distance_and_velocity_boundaries_are_strict() {
        listOf(
            59.99f to Release.CANCEL, 60f to Release.CANCEL, 60.01f to Release.OPEN,
        ).forEach { (dy, expected) -> assertEquals(expected, release(Axis.Y, dyDp = dy)) }
        listOf(
            0.4499f to Release.CANCEL, 0.45f to Release.CANCEL, 0.4501f to Release.OPEN,
        ).forEach { (vy, expected) -> assertEquals(expected, release(Axis.Y, vyDpPerMs = vy)) }
        assertEquals(Release.CANCEL, release(Axis.Y, dyDp = -100f, vyDpPerMs = -10f))
        assertEquals(Release.OPEN, release(Axis.Y, dyDp = 60.01f, vyDpPerMs = -10f))
        assertEquals(Release.OPEN, release(Axis.Y, dyDp = -100f, vyDpPerMs = 0.4501f))
    }

    @Test
    fun fast_flicks_only_open_and_release_vocabulary_has_no_send_outcome() {
        listOf(0f, 61f, 100f).forEach { dy ->
            assertEquals(Release.OPEN, release(Axis.Y, dyDp = dy, vyDpPerMs = 100f))
        }
        assertEquals(
            setOf("SAVE", "PASS", "CANCEL", "OPEN", "CLOSE", "STAY_OPEN"),
            Release.entries.map { it.name }.toSet(),
        )
    }

    @Test
    fun open_vertical_distance_and_velocity_boundaries_are_strict() {
        listOf(
            -49.99f to Release.STAY_OPEN, -50f to Release.STAY_OPEN, -50.01f to Release.CLOSE,
        ).forEach { (dy, expected) -> assertEquals(expected, release(Axis.Y, dyDp = dy, isOpen = true)) }
        listOf(
            -0.4499f to Release.STAY_OPEN, -0.45f to Release.STAY_OPEN, -0.4501f to Release.CLOSE,
        ).forEach { (vy, expected) -> assertEquals(expected, release(Axis.Y, vyDpPerMs = vy, isOpen = true)) }
        assertEquals(Release.STAY_OPEN, release(Axis.Y, dyDp = -1f, vyDpPerMs = -0.1f, isOpen = true))
        assertEquals(Release.STAY_OPEN, release(Axis.Y, dyDp = 100f, vyDpPerMs = 10f, isOpen = true))
        assertEquals(Release.CLOSE, release(Axis.Y, dyDp = -51f, vyDpPerMs = 10f, isOpen = true))
        assertEquals(Release.CLOSE, release(Axis.Y, dyDp = 100f, vyDpPerMs = -1f, isOpen = true))
    }

    @Test
    fun open_card_ignores_all_horizontal_releases() {
        listOf(-1000f, -90f, 0f, 90f, 1000f).forEach { dx ->
            listOf(-100f, 0f, 100f).forEach { vx ->
                assertEquals(Release.STAY_OPEN, release(Axis.X, dxDp = dx, vxDpPerMs = vx, isOpen = true))
            }
        }
    }

    @Test
    fun taps_depend_only_on_open_state_and_media_target() {
        listOf(false, true).forEach { onMedia ->
            assertEquals(Release.CANCEL, release(null, tapOnMedia = onMedia))
        }
        assertEquals(Release.CLOSE, release(null, isOpen = true, tapOnMedia = true))
        assertEquals(Release.STAY_OPEN, release(null, isOpen = true, tapOnMedia = false))
        assertEquals(Release.CANCEL, release(null, dxDp = 100f, vyDpPerMs = 10f))
    }

    @Test
    fun axis_locks_at_platform_slop_with_ties_and_open_cards_on_y() {
        assertEquals(null, CardGestureResolver.resolveAxis(9.99f, 0f, 10f, false))
        assertEquals(Axis.X, CardGestureResolver.resolveAxis(10f, 0f, 10f, false))
        assertEquals(Axis.Y, CardGestureResolver.resolveAxis(6f, 8f, 10f, false))
        assertEquals(Axis.Y, CardGestureResolver.resolveAxis(10f, 10f, 10f, false))
        assertEquals(Axis.X, CardGestureResolver.resolveAxis(-10f, 1f, 10f, false))
        assertEquals(null, CardGestureResolver.resolveAxis(10f, 0f, 20f, false))
        assertEquals(Axis.X, CardGestureResolver.resolveAxis(20f, 0f, 20f, false))
        assertEquals(null, CardGestureResolver.resolveAxis(9f, 0f, 10f, true))
        listOf(-100f, 0f, 100f).forEach { dx ->
            assertEquals(Axis.Y, CardGestureResolver.resolveAxis(dx, -10f, 10f, true))
        }
    }

    private fun release(
        axis: Axis?,
        dxDp: Float = 0f,
        dyDp: Float = 0f,
        vxDpPerMs: Float = 0f,
        vyDpPerMs: Float = 0f,
        isOpen: Boolean = false,
        tapOnMedia: Boolean = false,
    ): Release = CardGestureResolver.resolveRelease(axis, dxDp, dyDp, vxDpPerMs, vyDpPerMs, isOpen, tapOnMedia)
}
