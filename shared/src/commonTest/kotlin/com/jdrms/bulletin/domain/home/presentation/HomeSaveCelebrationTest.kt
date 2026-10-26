package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomeSaveCelebrationTest {
    @Test
    fun target_uses_window_coordinate_difference_and_density_or_falls_back() {
        val target = homeSaveTarget(Rect(900f, 1800f, 1000f, 1900f), Offset(500f, 900f), 2f)
        assertEquals(HomeSaveTarget(225f, 475f), target)
        val pose = homeCardPose(100f, HomeDepartureKind.SAVE, 1f, 360f, target)
        assertEquals(225f, pose.xDp)
        assertEquals(475f, pose.yDp)
        assertNull(homeSaveTarget(null, Offset.Zero, 2f))
        assertNull(homeSaveTarget(Rect.Zero, Offset.Zero, 2f))
        assertEquals(100f, homeCardPose(100f, HomeDepartureKind.SAVE, 1f, 360f).xDp)
        assertEquals(0f, homeCardPose(100f, HomeDepartureKind.PASS, 1f, 360f, target).yDp)
    }

    @Test
    fun confirmed_effect_waits_only_for_its_own_save_flight() {
        val effect = HomeEffect.SaveConfirmed(2, HomeItemId("a"))
        val departing = HomeInteractionState(
            card = CardState.Leaving,
            departure = HomeDeparture(1, effect.listingId, HomeDepartureKind.SAVE)
        )
        assertFalse(homeSaveCelebrationReady(effect, departing))
        assertTrue(homeSaveCelebrationReady(effect, HomeInteractionState()))
        assertTrue(
            homeSaveCelebrationReady(
                effect,
                departing.copy(
                    departure = HomeDeparture(3, HomeItemId("b"), HomeDepartureKind.SAVE)
                )
            )
        )
    }
}
