package com.jdrms.bulletin.domain.home.presentation

import bulletin.shared.generated.resources.*
import com.jdrms.bulletin.domain.home.domain.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeActionButtonUiTest {
    @Test
    fun buttons_follow_prototype_order_and_guest_hint() {
        val buttons = CardState.Idle.availableActions(HomeActionContext(true, false, true, true)).toButtonUi()
        assertEquals(listOf(CardAction.Pass, CardAction.Message, CardAction.Save), buttons.map { it.action })
        assertTrue(buttons[0].enabled)
        assertFalse(buttons[2].enabled)
        assertEquals(Res.string.home_signed_out, buttons[2].disabledHint)
        assertEquals(Res.string.home_button_message, buttons[1].label)
    }

    @Test
    fun buttons_are_visible_but_disabled_during_departure() {
        val buttons = CardState.Leaving.availableActions(HomeActionContext(true, true, true, true)).toButtonUi()
        assertTrue(buttons.none { it.enabled })
        assertEquals(1f, homeActionRowAlpha(0f))
    }

    @Test
    fun fade_finishes_at_forty_percent_tray_progress() {
        assertEquals(1f, homeActionRowAlpha(-1f))
        assertEquals(.5f, homeActionRowAlpha(.2f))
        assertEquals(0f, homeActionRowAlpha(.4f))
        assertEquals(0f, homeActionRowAlpha(1f))
    }

    @Test
    fun unavailable_adapters_have_distinct_hints() {
        val buttons = CardState.Idle.availableActions(HomeActionContext(true, true, false, false)).toButtonUi()
        assertEquals(Res.string.home_message_not_configured, buttons[1].disabledHint)
        assertEquals(Res.string.home_save_not_configured, buttons[2].disabledHint)
    }
}
