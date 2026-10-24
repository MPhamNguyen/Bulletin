@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.jdrms.bulletin.domain.home.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.application.SaveListingFailure
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import com.jdrms.bulletin.domain.home.domain.model.SwipeDecision
import com.jdrms.bulletin.domain.home.domain.model.SwipeVerdict
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeSaveInteractionTest {
    @Test
    fun retry_keeps_original_listing_after_deck_advance_and_effect_ack_is_independent() = interactionTest { f ->
        f.saveResult = { Result.Error(SaveListingFailure.Network()) }
        f.vm.saveCurrentCard()
        val effect = f.interaction.pendingEffects.single() as HomeEffect.SaveRetryOffered
        assertEquals(HomeItemId("a"), effect.listingId)
        f.vm.onEffectConsumed(effect.id + 100)
        assertEquals(listOf(effect), f.interaction.pendingEffects)
        f.vm.onEffectConsumed(effect.id)
        f.vm.onEffectConsumed(effect.id)
        f.exit()
        f.vm.openTray()
        f.vm.onDraftChanged("next listing draft")
        f.saveResult = { Result.Success(Unit) }
        f.vm.retrySave(effect.listingId)
        assertEquals(listOf("viewer" to HomeItemId("a"), "viewer" to HomeItemId("a")), f.saves)
        assertEquals("next listing draft", f.interaction.tray.draft)
        assertEquals(CardState.Open, f.interaction.card)
        assertEquals(effect.listingId, (f.interaction.pendingEffects.single() as HomeEffect.SaveConfirmed).listingId)
        f.vm.dismissSaveRetry(effect.listingId)
        assertTrue(f.decisions.removals.isEmpty())
    }

    @Test
    fun dismissal_removes_only_save_and_never_a_later_pass() = interactionTest { f ->
        f.saveResult = { Result.Error(SaveListingFailure.Unknown()) }
        f.vm.saveCurrentCard()
        f.vm.dismissSaveRetry(HomeItemId("a"))
        assertEquals(emptySet(), f.decisions.getExcludedListingIds("viewer", SwipeVerdict.SAVE).getOrNull())
        f.exit()
        f.vm.saveCurrentCard()
        f.decisions.record("viewer", SwipeDecision(HomeItemId("b"), SwipeVerdict.PASS))
        f.vm.dismissSaveRetry(HomeItemId("b"))
        f.vm.dismissSaveRetry(HomeItemId("b"))
        assertEquals(setOf(HomeItemId("b")), f.decisions.getExcludedListingIds("viewer", SwipeVerdict.PASS).getOrNull())
        assertEquals(2, f.decisions.removals.size)
        assertTrue(f.interaction.pendingEffects.isEmpty())
    }

    @Test
    fun retry_duplicates_are_blocked_while_retry_is_suspended() = interactionTest { f ->
        f.saveResult = { Result.Error(SaveListingFailure.Network()) }
        f.vm.saveCurrentCard()
        val result = CompletableDeferred<Result<Unit>>()
        f.saveResult = { result.await() }
        f.vm.retrySave(HomeItemId("a"))
        f.vm.retrySave(HomeItemId("a"))
        f.vm.dismissSaveRetry(HomeItemId("a"))
        assertEquals(2, f.saves.size)
        assertTrue(f.decisions.removals.isEmpty())
        result.complete(Result.Error(SaveListingFailure.Network()))
        advanceUntilIdle()
        assertEquals(1, f.interaction.pendingEffects.size)
    }

    @Test
    fun save_cancellation_propagates_without_retry_or_error_effect() = interactionTest { f ->
        f.saveResult = { throw CancellationException("cancelled") }
        f.vm.saveCurrentCard()
        advanceUntilIdle()
        assertEquals(CardState.Leaving, f.interaction.card)
        assertTrue(f.interaction.pendingEffects.isEmpty())
        assertEquals(null, f.interaction.tray.error)
        f.exit()
        assertEquals(HomeItemId("b"), f.interaction.listingId)
    }

    @Test
    fun non_cooperative_old_save_failure_cannot_emit_after_refresh_or_account_switch() = interactionTest { f ->
        val result = CompletableDeferred<Result<Unit>>()
        f.saveResult = { withContext(NonCancellable) { result.await() } }
        f.vm.saveCurrentCard()
        f.vm.refresh()
        f.signIn("other")
        runCurrent()
        result.complete(Result.Error(SaveListingFailure.Network()))
        advanceUntilIdle()
        assertTrue(f.interaction.pendingEffects.isEmpty())
        assertEquals(HomeItemId("a"), f.interaction.listingId)
        f.vm.retrySave(HomeItemId("a"))
        f.vm.dismissSaveRetry(HomeItemId("a"))
        assertEquals(1, f.saves.size)
        assertTrue(f.decisions.removals.isEmpty())
    }

    @Test
    fun decision_storage_failure_offers_retry_without_calling_bookmark_port() = interactionTest { f ->
        f.decisions.recordFailure = IllegalStateException("private storage error")
        f.vm.saveCurrentCard()
        assertTrue(f.saves.isEmpty())
        assertEquals(HomeItemId("a"), (f.interaction.pendingEffects.single() as HomeEffect.SaveRetryOffered).listingId)
        assertFalse(f.interaction.toString().contains("private storage error"))
        f.decisions.recordFailure = null
        f.vm.retrySave(HomeItemId("a"))
        assertEquals(1, f.saves.size)
        assertEquals(HomeItemId("a"), (f.interaction.pendingEffects.single() as HomeEffect.SaveConfirmed).listingId)
    }

    @Test
    fun dismissal_storage_failure_is_typed_and_does_not_throw() = interactionTest { f ->
        f.saveResult = { Result.Error(SaveListingFailure.Network()) }
        f.decisions.removeFailure = IllegalStateException("private failure")
        f.vm.saveCurrentCard()
        f.vm.dismissSaveRetry(HomeItemId("a"))
        assertEquals(HomeInteractionError.DECISION_STORAGE, f.interaction.tray.error)
        assertTrue(f.interaction.pendingEffects.isEmpty())
        assertFalse(f.interaction.toString().contains("private failure"))
    }

    @Test
    fun late_save_failure_offers_original_retry_without_overwriting_later_card_draft() = interactionTest { f ->
        val result = CompletableDeferred<Result<Unit>>()
        f.saveResult = { result.await() }
        f.vm.saveCurrentCard()
        f.exit()
        f.vm.openTray()
        f.vm.onDraftChanged("draft for b")
        result.complete(Result.Error(SaveListingFailure.Network()))
        advanceUntilIdle()
        assertEquals(HomeItemId("b"), f.interaction.listingId)
        assertEquals(HomeTrayState(draft = "draft for b"), f.interaction.tray)
        assertEquals(HomeItemId("a"), (f.interaction.pendingEffects.single() as HomeEffect.SaveRetryOffered).listingId)
    }

    @Test
    fun returning_to_same_account_does_not_revive_an_old_save_attempt() = interactionTest { f ->
        val result = CompletableDeferred<Result<Unit>>()
        f.saveResult = { withContext(NonCancellable) { result.await() } }
        f.vm.saveCurrentCard()
        f.signIn("other")
        runCurrent()
        f.signIn("viewer")
        runCurrent()
        result.complete(Result.Error(SaveListingFailure.Network()))
        advanceUntilIdle()
        assertTrue(f.interaction.pendingEffects.isEmpty())
        assertEquals(CardState.Idle, f.interaction.card)
        f.vm.retrySave(HomeItemId("a"))
        assertEquals(1, f.saves.size)
    }

    @Test
    fun delayed_cleanup_failure_does_not_put_an_error_on_the_next_card() = interactionTest { f ->
        val removed = CompletableDeferred<Unit>()
        f.decisions.beforeRemove = { removed.await() }
        f.decisions.removeFailure = IllegalStateException("private storage error")
        f.saveResult = { Result.Error(SaveListingFailure.Network()) }
        f.vm.saveCurrentCard()
        f.vm.dismissSaveRetry(HomeItemId("a"))
        f.exit()
        f.vm.openTray()
        f.vm.onDraftChanged("next card draft")
        removed.complete(Unit)
        advanceUntilIdle()
        assertEquals(HomeTrayState(draft = "next card draft"), f.interaction.tray)
        assertTrue(f.interaction.pendingEffects.isEmpty())
    }

    @Test
    fun pending_effects_are_ordered_and_have_unique_ids_across_acknowledgement_and_reset() = interactionTest { f ->
        f.saveResult = { Result.Error(SaveListingFailure.Network()) }
        f.vm.saveCurrentCard()
        val first = f.interaction.pendingEffects.single()
        f.exit()
        f.vm.saveCurrentCard()
        val effects = f.interaction.pendingEffects
        assertEquals(listOf("a", "b"), effects.map { (it as HomeEffect.SaveRetryOffered).listingId.value })
        assertTrue(effects[0].id < effects[1].id)
        f.vm.onEffectConsumed(first.id)
        assertEquals(listOf(effects[1]), f.interaction.pendingEffects)
        f.vm.refresh()
        advanceUntilIdle()
        assertTrue(f.interaction.pendingEffects.isEmpty())
        assertEquals(2, f.decisions.removals.size)
    }
}
