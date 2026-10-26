@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.jdrms.bulletin.domain.home.presentation

import androidx.lifecycle.ViewModelStore
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.application.SaveListingFailure
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomeInteractionResetTest {
    @Test
    fun listing_changes_exhaustion_and_append_reset_drafts_without_changing_cursor() = interactionTest { f ->
        f.feedResult = { homePage("a", cursor = "remote-cursor") }
        f.vm.refresh()
        advanceUntilIdle()
        val cursor = f.vm.uiState.value.nextCursor
        f.vm.openTray()
        f.vm.onDraftChanged("draft for a")
        f.vm.closeTray()
        f.vm.passCurrentCard()
        f.exit()
        assertNull(f.interaction.listingId)
        f.vm.openTray()
        f.vm.onDraftChanged("ignored on exhausted deck")
        assertFalse(f.interaction.messageAvailable)
        assertFalse(f.interaction.saveAvailable)
        assertEquals(CardState.Idle, f.interaction.card)
        assertEquals(HomeTrayState(), f.interaction.tray)
        f.feedResult = { homePage("b", "c") }
        f.vm.loadNextPage()
        advanceUntilIdle()
        assertEquals(cursor, f.cursors.last())
        assertEquals(HomeItemId("b"), f.interaction.listingId)
        assertEquals(0, f.vm.uiState.value.currentIndex)
        assertEquals(HomeTrayState(), f.interaction.tray)
    }

    @Test
    fun append_preserves_departing_card_until_exit_then_advances_to_correct_next_card() = interactionTest { f ->
        f.feedResult = { homePage("a", "b", cursor = "remote-cursor") }
        f.vm.refresh()
        advanceUntilIdle()
        f.vm.saveCurrentCard()
        val departure = requireNotNull(f.interaction.departure)
        f.feedResult = { homePage("c") }
        f.vm.loadNextPage()
        advanceUntilIdle()
        assertEquals(departure, f.interaction.departure)
        assertEquals(HomeItemId("a"), f.interaction.listingId)
        assertEquals(CardState.Leaving, f.interaction.card)
        f.exit()
        assertEquals(HomeItemId("b"), f.interaction.listingId)
    }

    @Test
    fun refresh_replay_and_listing_signal_reset_even_when_listing_id_stays_the_same() = interactionTest { f ->
        val resets: List<suspend () -> Unit> = listOf(
            { f.vm.refresh() },
            { f.vm.replayLoadedCards() },
            { f.signal.emit() }
        )
        resets.forEach { reset ->
            f.vm.openTray()
            f.vm.onDraftChanged("private draft")
            reset()
            advanceUntilIdle()
            assertEquals(HomeItemId("a"), f.interaction.listingId)
            assertEquals(CardState.Idle, f.interaction.card)
            assertEquals(HomeTrayState(), f.interaction.tray)
            assertTrue(f.interaction.pendingEffects.isEmpty())
        }
    }

    @Test
    fun account_switch_and_sign_out_clear_draft_effects_and_old_retry_authority() = interactionTest { f ->
        f.saveResult = { Result.Error(SaveListingFailure.Network()) }
        f.vm.saveCurrentCard()
        f.exit()
        f.vm.openTray()
        f.vm.onDraftChanged("viewer draft")
        f.signIn("other")
        advanceUntilIdle()
        assertEquals(HomeTrayState(), f.interaction.tray)
        assertTrue(f.interaction.pendingEffects.isEmpty())
        f.vm.retrySave(HomeItemId("a"))
        assertEquals(1, f.saves.size)
        f.vm.openTray()
        f.vm.onDraftChanged("other draft")
        f.unauthenticated()
        advanceUntilIdle()
        assertEquals(HomeTrayState(), f.interaction.tray)
        assertEquals(CardState.Idle, f.interaction.card)
        assertFalse(f.interaction.messageAvailable)
    }

    @Test
    fun non_cooperative_old_send_success_cannot_mutate_next_session_card_or_draft() = interactionTest { f ->
        val result = CompletableDeferred<Result<Unit>>()
        f.sendResult = { withContext(NonCancellable) { result.await() } }
        f.vm.openTray()
        f.vm.onDraftChanged("old draft")
        f.vm.sendMessage()
        f.signIn("other")
        runCurrent()
        f.vm.openTray()
        f.vm.onDraftChanged("new draft")
        result.complete(Result.Success(Unit))
        advanceUntilIdle()
        assertEquals(CardState.Open, f.interaction.card)
        assertEquals(HomeTrayState(draft = "new draft"), f.interaction.tray)
        assertNull(f.interaction.departure)
        assertEquals(1, f.sends.size)
    }

    @Test
    fun refresh_cancels_send_and_late_failure_cannot_restore_old_draft_on_same_listing() = interactionTest { f ->
        val result = CompletableDeferred<Result<Unit>>()
        f.sendResult = { withContext(NonCancellable) { result.await() } }
        f.vm.openTray()
        f.vm.onDraftChanged("old draft")
        f.vm.sendMessage()
        f.vm.refresh()
        runCurrent()
        result.complete(Result.Error(IllegalStateException("private error")))
        advanceUntilIdle()
        assertEquals(CardState.Idle, f.interaction.card)
        assertEquals(HomeTrayState(), f.interaction.tray)
        assertTrue(f.interaction.pendingEffects.isEmpty())
    }

    @Test
    fun recreated_viewmodel_has_no_draft_or_pending_effects_from_destroyed_instance() = interactionTest { f ->
        f.saveResult = { Result.Error(SaveListingFailure.Network()) }
        f.vm.saveCurrentCard()
        f.exit()
        f.vm.openTray()
        f.vm.onDraftChanged("old draft")
        f.store.clear()
        val newStore = ViewModelStore()
        try {
            val recreated = f.createVm(newStore)
            advanceUntilIdle()
            assertTrue(f.vm.uiState.value.interaction.pendingEffects.isEmpty())
            assertFalse(f.vm.uiState.value.interaction.messageAvailable)
            assertEquals(HomeTrayState(), recreated.uiState.value.interaction.tray)
            assertTrue(recreated.uiState.value.interaction.pendingEffects.isEmpty())
            assertEquals(CardState.Idle, recreated.uiState.value.interaction.card)
        } finally {
            newStore.clear()
        }
    }

    @Test
    fun legacy_listing_advance_clears_draft_and_stale_exit_cannot_finish_new_departure() = interactionTest { f ->
        f.vm.openTray()
        f.vm.onDraftChanged("draft")
        f.vm.advanceCard(0)
        assertEquals(HomeItemId("b"), f.interaction.listingId)
        assertEquals(HomeTrayState(), f.interaction.tray)
        f.vm.passCurrentCard()
        val old = requireNotNull(f.interaction.departure).id
        f.vm.refresh()
        advanceUntilIdle()
        f.vm.passCurrentCard()
        f.vm.onExitCompleted(old)
        assertEquals(0, f.vm.uiState.value.currentIndex)
        assertEquals(CardState.Leaving, f.interaction.card)
    }
}
