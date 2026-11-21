package com.jdrms.bulletin.domain.messages.presentation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConversationBackSwipeDecisionTest {
    @Test
    fun deliberateRightwardDragReturnsToInbox() {
        assertTrue(shouldReturnToInbox(96f, 0f))
        assertFalse(shouldReturnToInbox(95f, 0f))
    }

    @Test
    fun quickRightwardFlickRequiresMinimumTravel() {
        assertTrue(shouldReturnToInbox(32f, 900f))
        assertFalse(shouldReturnToInbox(31f, 2000f))
        assertFalse(shouldReturnToInbox(40f, 899f))
    }

    @Test
    fun leftwardAndVerticalGesturesStayInChat() {
        assertFalse(shouldReturnToInbox(-150f, -1200f))
        assertFalse(shouldReturnToInbox(0f, 0f))
    }

    @Test
    fun reversingDirectionCancelsBackSwipe() {
        assertFalse(shouldReturnToInbox(120f, -500f))
    }
}
