@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.jdrms.bulletin.domain.home.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.application.SaveListingFailure
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HomeSaveConfirmedTest {
    @Test
    fun success_is_once_and_acknowledgement_is_idempotent_without_advancing_deck() = interactionTest { f ->
        f.vm.saveCurrentCard()
        val effect = f.interaction.pendingEffects.single() as HomeEffect.SaveConfirmed
        assertEquals(HomeItemId("a"), effect.listingId)
        f.vm.saveCurrentCard()
        assertEquals(listOf(effect), f.interaction.pendingEffects)
        assertEquals(CardState.Leaving, f.interaction.card)
        assertEquals(0, f.vm.uiState.value.currentIndex)
        f.exit()
        assertEquals(listOf(effect), f.interaction.pendingEffects)
        f.vm.onEffectConsumed(effect.id)
        f.vm.onEffectConsumed(effect.id)
        assertTrue(f.interaction.pendingEffects.isEmpty())
        assertEquals(1, f.vm.uiState.value.currentIndex)
    }

    @Test
    fun optimistic_exit_finishes_before_late_success_and_signal_keeps_original_listing() = interactionTest { f ->
        val result = CompletableDeferred<Result<Unit>>()
        f.saveResult = { result.await() }
        f.vm.saveCurrentCard()
        assertEquals(HomeDepartureKind.SAVE, f.interaction.departure?.kind)
        assertTrue(f.interaction.pendingEffects.isEmpty())
        f.exit()
        assertEquals(HomeItemId("b"), f.interaction.listingId)
        result.complete(Result.Success(Unit))
        advanceUntilIdle()
        assertEquals(HomeItemId("a"), (f.interaction.pendingEffects.single() as HomeEffect.SaveConfirmed).listingId)
        assertEquals(HomeItemId("b"), f.interaction.listingId)
    }

    @Test
    fun retry_success_confirms_once_with_a_new_effect_id() = interactionTest { f ->
        f.saveResult = { Result.Error(SaveListingFailure.Network()) }
        f.vm.saveCurrentCard()
        val failure = f.interaction.pendingEffects.single() as HomeEffect.SaveRetryOffered
        assertTrue(f.interaction.pendingEffects.none { it is HomeEffect.SaveConfirmed })
        f.exit()
        f.saveResult = { Result.Success(Unit) }
        f.vm.retrySave(failure.listingId)
        f.vm.retrySave(failure.listingId)
        val confirmed = f.interaction.pendingEffects.single() as HomeEffect.SaveConfirmed
        assertEquals(failure.listingId, confirmed.listingId)
        assertTrue(confirmed.id > failure.id)
        assertEquals(CardState.Idle, f.interaction.card)
        assertEquals(null, f.interaction.departure)
    }

    @Test
    fun cancellation_never_confirms() = interactionTest { f ->
        f.saveResult = { throw CancellationException("cancelled") }
        f.vm.saveCurrentCard()
        advanceUntilIdle()
        assertTrue(f.interaction.pendingEffects.isEmpty())
    }

    @Test
    fun stale_viewer_or_reset_attempt_never_confirms() = interactionTest { f ->
        val resets: List<() -> Unit> = listOf({ f.signIn("other") }, { f.vm.refresh() })
        resets.forEach { reset ->
            val result = CompletableDeferred<Result<Unit>>()
            f.saveResult = { withContext(NonCancellable) { result.await() } }
            f.vm.saveCurrentCard()
            reset()
            runCurrent()
            result.complete(Result.Success(Unit))
            advanceUntilIdle()
            assertTrue(f.interaction.pendingEffects.isEmpty())
        }
    }
}
