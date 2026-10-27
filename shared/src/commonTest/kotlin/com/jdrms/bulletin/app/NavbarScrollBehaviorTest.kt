package com.jdrms.bulletin.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import com.jdrms.bulletin.app.navigation.AppDestination
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

    @Test
    fun postScrollAtTop_revealsNavbar() {
        val behavior = NavbarScrollBehavior(thresholdPx = 8f)
        behavior.onScrollDelta(9f)
        val connection = NavbarNestedScrollConnection(behavior)

        connection.onPostScroll(
            consumed = Offset.Zero,
            available = Offset(0f, 1f),
            source = NestedScrollSource.UserInput
        )

        assertTrue(behavior.isVisible)
    }

    @Test
    fun marketplaceAndMessagesOptIntoHiding() {
        assertFalse(AppDestination.HOME.hidesOnScroll)
        assertTrue(AppDestination.MARKETPLACE.hidesOnScroll)
        assertFalse(AppDestination.LISTINGS.hidesOnScroll)
        assertTrue(AppDestination.MESSAGES.hidesOnScroll)
        assertFalse(AppDestination.PROFILE.hidesOnScroll)
    }

    @Test
    fun destinationChange_revealsNavbarAndResetsScrollDirection() {
        val behavior = NavbarScrollBehavior(thresholdPx = 8f)
        behavior.onScrollDelta(9f)

        behavior.onDestinationChanged()
        behavior.onScrollDelta(3f)

        assertTrue(behavior.isVisible)
    }
}
