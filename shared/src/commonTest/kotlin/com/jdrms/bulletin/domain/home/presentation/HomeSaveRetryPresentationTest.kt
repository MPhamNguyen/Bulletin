@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.jdrms.bulletin.domain.home.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.application.SaveListingFailure
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import com.jdrms.bulletin.domain.home.domain.model.SwipeDecision
import com.jdrms.bulletin.domain.home.domain.model.SwipeVerdict
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeSaveRetryPresentationTest {
    @Test
    fun leaving_home_dismisses_acknowledged_offer_and_return_does_not_resurrect() = interactionTest { f ->
        f.saveResult = { Result.Error(SaveListingFailure.Network()) }
        f.vm.saveCurrentCard()
        val presentation = HomeSaveRetryPresentation()
        presentation.synchronize(true, f.vm.uiState.value, f.vm)
        val offer = presentation.offers.single()
        assertTrue(presentation.shown(offer.effect.id))
        f.vm.onEffectConsumed(offer.effect.id)
        assertFalse(presentation.shown(offer.effect.id))
        f.exit()
        presentation.synchronize(false, f.vm.uiState.value, f.vm)
        assertEquals(listOf(HomeItemId("a")), f.decisions.removals.map { it.second })
        assertEquals(emptySet(), f.decisions.getExcludedListingIds("viewer", SwipeVerdict.SAVE).getOrNull())
        presentation.synchronize(true, f.vm.uiState.value, f.vm)
        assertTrue(presentation.offers.isEmpty())
    }

    @Test
    fun navigation_cleanup_never_removes_a_newer_pass() = interactionTest { f ->
        f.saveResult = { Result.Error(SaveListingFailure.Network()) }
        f.vm.saveCurrentCard()
        f.decisions.record("viewer", SwipeDecision(HomeItemId("a"), SwipeVerdict.PASS))
        val presentation = HomeSaveRetryPresentation()
        presentation.synchronize(false, f.vm.uiState.value, f.vm)
        assertEquals(setOf(HomeItemId("a")), f.decisions.getExcludedListingIds("viewer", SwipeVerdict.PASS).getOrNull())
        assertTrue(presentation.offers.isEmpty())
    }

    @Test
    fun recreation_restores_shown_offer_without_dismissal_or_duplicate_ack() = interactionTest { f ->
        f.saveResult = { Result.Error(SaveListingFailure.Network()) }
        f.vm.saveCurrentCard()
        val before = HomeSaveRetryPresentation()
        before.synchronize(true, f.vm.uiState.value, f.vm)
        val id = before.offers.single().effect.id
        before.shown(id)
        f.vm.onEffectConsumed(id)
        val after = HomeSaveRetryPresentation.restore(before.snapshot())
        after.synchronize(true, f.vm.uiState.value, f.vm)
        assertEquals(before.offers, after.offers)
        assertFalse(after.shown(id))
        assertTrue(f.decisions.removals.isEmpty())
        after.synchronize(false, f.vm.uiState.value, f.vm)
        assertEquals(1, f.decisions.removals.size)
    }

    @Test
    fun unseen_offers_and_late_failures_while_away_are_dismissed_without_ack() = interactionTest { f ->
        f.saveResult = { Result.Error(SaveListingFailure.Network()) }
        f.vm.saveCurrentCard()
        val effect = f.interaction.pendingEffects.single() as HomeEffect.SaveRetryOffered
        val presentation = HomeSaveRetryPresentation()
        presentation.synchronize(false, f.vm.uiState.value, f.vm)
        assertTrue(f.interaction.pendingEffects.isEmpty())
        assertTrue(presentation.offers.isEmpty())
        assertEquals(null, presentation.take(effect.id))
    }

    @Test
    fun refresh_invalidates_saved_delivery_bookkeeping() = interactionTest { f ->
        f.saveResult = { Result.Error(SaveListingFailure.Network()) }
        f.vm.saveCurrentCard()
        val presentation = HomeSaveRetryPresentation()
        presentation.synchronize(true, f.vm.uiState.value, f.vm)
        f.vm.refresh()
        presentation.synchronize(true, f.vm.uiState.value, f.vm)
        assertTrue(presentation.offers.isEmpty())
    }
}
