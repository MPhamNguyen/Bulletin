package com.jdrms.bulletin.app

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavbarScrollBehaviorTest {
    @Test
    fun scrollingDownPastThreshold_hidesNavbar() {
        val behavior = NavbarScrollBehavior(thresholdPx = 8f)

        behavior.onScrollDelta(5f)
        assertTrue(behavior.isVisible)

        behavior.onScrollDelta(4f)

        assertFalse(behavior.isVisible)
    }

    @Test
    fun scrollingUpPastThreshold_revealsNavbar() {
        val behavior = NavbarScrollBehavior(thresholdPx = 8f)
        behavior.onScrollDelta(9f)

        behavior.onScrollDelta(-8f)

        assertTrue(behavior.isVisible)
    }

    @Test
    fun smallDirectionChanges_doNotAccumulateAcrossDirections() {
        val behavior = NavbarScrollBehavior(thresholdPx = 8f)

        behavior.onScrollDelta(5f)
        behavior.onScrollDelta(-5f)

        assertTrue(behavior.isVisible)
    }

    @Test
    fun reveal_resetsPendingScrollDistance() {
        val behavior = NavbarScrollBehavior(thresholdPx = 8f)
        behavior.onScrollDelta(6f)

        behavior.reveal()
        behavior.onScrollDelta(3f)

        assertTrue(behavior.isVisible)
    }
}
