package com.jdrms.bulletin.domain.home.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeNextCardPoseTest {
    @Test
    fun dragPromotionCapsAtFiftyFivePercentInEitherDirection() {
        assertEquals(0f, homeDragPromotion(0f))
        assertEquals(0.275f, homeDragPromotion(45f))
        assertEquals(0.55f, homeDragPromotion(90f))
        assertEquals(0.55f, homeDragPromotion(-900f))
    }

    @Test
    fun exitCompletesPromotionAtFullSizeAndWithoutShade() {
        assertEquals(HomeNextCardPose(0.94f, 0.3f), homeNextCardPose(0f))
        assertEquals(HomeNextCardPose(1f, 0f), homeNextCardPose(1f))
        assertEquals(HomeNextCardPose(1f, 0f), homeNextCardPose(2f))
        assertEquals(HomeNextCardPose(0.94f, 0.3f), homeNextCardPose(-1f))
    }

    @Test
    fun partialPromotionInterpolatesSizeAndShadeTogether() {
        assertEquals(0.973f, homeNextCardPose(0.55f).scale, 0.00001f)
        assertEquals(0.135f, homeNextCardPose(0.55f).shadeAlpha, 0.00001f)
    }
}
