package com.jdrms.bulletin.domain.home.presentation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeDescriptionScrollTest {
    @Test
    fun only_expanded_content_that_can_scroll_in_finger_direction_claims() {
        assertFalse(homeDescriptionCanScroll(false, 10f, true, true))
        assertFalse(homeDescriptionCanScroll(true, 10f, false, false))
        assertTrue(homeDescriptionCanScroll(true, 10f, true, false))
        assertFalse(homeDescriptionCanScroll(true, -10f, true, false))
        assertTrue(homeDescriptionCanScroll(true, -10f, false, true))
        assertFalse(homeDescriptionCanScroll(true, 10f, false, true))
        assertFalse(homeDescriptionCanScroll(true, 0f, true, true))
    }
}
