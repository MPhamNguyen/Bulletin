@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.jdrms.bulletin.domain.home.presentation

import com.jdrms.bulletin.domain.home.domain.model.CardAction
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HomeActionRowCommandTest {
    @Test
    fun button_then_gesture_then_button_cannot_create_duplicate_save_or_pass() = interactionTest { f ->
        f.vm.perform(CardAction.Save)
        val departure = f.interaction.departure
        f.vm.onGestureEvent(CardEvent.DRAG_X)
        f.vm.saveCurrentCard()
        f.vm.perform(CardAction.Pass)
        assertEquals(departure, f.interaction.departure)
        assertEquals(1, f.saves.size)
        assertEquals(0, f.vm.uiState.value.currentIndex)
        f.exit()
        assertEquals(1, f.vm.uiState.value.currentIndex)
    }

    @Test
    fun guest_save_is_rejected_but_pass_still_advances_on_exit() = interactionTest { f ->
        f.unauthenticated()
        advanceUntilIdle()
        f.vm.perform(CardAction.Save)
        assertTrue(f.saves.isEmpty())
        assertEquals(CardState.Idle, f.interaction.card)
        f.vm.perform(CardAction.Pass)
        assertEquals(HomeItemId("a"), f.interaction.departure?.listingId)
        f.exit()
        assertEquals(HomeItemId("b"), f.interaction.listingId)
    }
}
