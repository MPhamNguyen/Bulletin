@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.jdrms.bulletin.domain.home.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import com.jdrms.bulletin.domain.home.domain.model.SwipeVerdict
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomeCardInteractionTest {
    @Test
    fun gestures_and_tray_commands_consume_accepted_and_rejected_machine_transitions() = interactionTest { f ->
        val vm = f.vm
        vm.onGestureEvent(CardEvent.DRAG_X)
        assertEquals(CardState.Swiping, f.interaction.card)
        vm.openTray()
        assertEquals(CardState.Swiping, f.interaction.card)
        vm.onGestureEvent(CardEvent.CANCEL)
        vm.onGestureEvent(CardEvent.DRAG_Y)
        assertEquals(CardState.Peeking, f.interaction.card)
        vm.passCurrentCard()
        vm.saveCurrentCard()
        assertTrue(f.saves.isEmpty())
        vm.openTray()
        assertEquals(CardState.Open, f.interaction.card)
        vm.onGestureEvent(CardEvent.CANCEL)
        assertEquals(CardState.Open, f.interaction.card)
        vm.closeTray()
        vm.closeTray()
        assertEquals(CardState.Idle, f.interaction.card)
        assertTrue(f.sends.isEmpty())
    }

    @Test
    fun only_exit_completion_applies_done_and_stale_tokens_cannot_advance_later_cards() = interactionTest { f ->
        f.vm.passCurrentCard()
        val first = assertNotNull(f.interaction.departure)
        f.vm.onGestureEvent(CardEvent.DONE)
        f.vm.advanceCard(0)
        f.vm.onExitCompleted(first.id + 1)
        assertEquals(0, f.vm.uiState.value.currentIndex)
        f.exit()
        assertEquals(HomeItemId("b"), f.interaction.listingId)
        assertEquals(CardState.Idle, f.interaction.card)
        f.vm.passCurrentCard()
        f.vm.onExitCompleted(first.id)
        assertEquals(1, f.vm.uiState.value.currentIndex)
        f.exit()
        assertEquals(2, f.vm.uiState.value.currentIndex)
    }

    @Test
    fun optimistic_save_records_scope_and_departs_before_network_completes() = interactionTest { f ->
        val result = CompletableDeferred<Result<Unit>>()
        f.saveResult = { result.await() }
        f.vm.saveCurrentCard()
        assertEquals(CardState.Leaving, f.interaction.card)
        assertEquals(HomeDepartureKind.SAVE, f.interaction.departure?.kind)
        assertEquals(listOf("viewer" to HomeItemId("a")), f.saves)
        assertEquals(SwipeVerdict.SAVE, f.decisions.records.single().second.verdict)
        assertEquals("viewer", f.decisions.records.single().first)
        f.vm.saveCurrentCard()
        f.vm.passCurrentCard()
        f.vm.openTray()
        f.vm.sendMessage()
        assertEquals(1, f.saves.size)
        assertTrue(f.sends.isEmpty())
        f.exit()
        assertEquals(HomeItemId("b"), f.interaction.listingId)
        result.complete(Result.Success(Unit))
        advanceUntilIdle()
        assertTrue(f.interaction.pendingEffects.isEmpty())
        assertEquals(setOf(HomeItemId("a")), f.decisions.getExcludedListingIds("viewer", SwipeVerdict.SAVE).getOrNull())
    }

    @Test
    fun pass_has_no_external_action_and_refresh_and_replay_keep_distinct_semantics() = interactionTest { f ->
        f.vm.passCurrentCard()
        f.exit()
        f.vm.saveCurrentCard()
        f.exit()
        assertTrue(f.sends.isEmpty())
        assertEquals(listOf("viewer" to HomeItemId("b")), f.saves)
        f.vm.refresh()
        advanceUntilIdle()
        assertEquals(listOf("c"), f.vm.uiState.value.feed.map { it.id.value })
        val requests = f.cursors.size
        f.vm.replayLoadedCards()
        advanceUntilIdle()
        assertEquals(requests, f.cursors.size)
        assertEquals(listOf("a", "c"), f.vm.uiState.value.feed.map { it.id.value })
        assertFalse(f.vm.uiState.value.prefetchEnabled)
    }

    @Test
    fun null_viewer_disables_save_and_message_with_notice_but_can_pass() = interactionTest { f ->
        f.unauthenticated()
        advanceUntilIdle()
        assertFalse(f.interaction.saveAvailable)
        assertFalse(f.interaction.messageAvailable)
        assertEquals(HomeInteractionError.NOT_SIGNED_IN, f.interaction.accountNotice)
        f.vm.openTray()
        f.vm.saveCurrentCard()
        f.vm.sendMessage()
        assertEquals(CardState.Idle, f.interaction.card)
        assertTrue(f.saves.isEmpty())
        assertTrue(f.sends.isEmpty())
        f.vm.passCurrentCard()
        f.exit()
        assertEquals(HomeItemId("b"), f.interaction.listingId)
    }

    @Test
    fun gesture_api_cannot_bypass_explicit_send_or_departure_handshake() = interactionTest { f ->
        CardEvent.entries.filter { it !in setOf(CardEvent.DRAG_X, CardEvent.DRAG_Y, CardEvent.CANCEL) }.forEach {
            f.vm.onGestureEvent(it)
            assertEquals(CardState.Idle, f.interaction.card)
        }
        f.vm.onGestureEvent(CardEvent.DRAG_Y)
        f.vm.onGestureEvent(CardEvent.CANCEL)
        assertEquals(CardState.Idle, f.interaction.card)
        assertNull(f.interaction.departure)
        assertTrue(f.sends.isEmpty())
        assertTrue(f.saves.isEmpty())
    }
}
