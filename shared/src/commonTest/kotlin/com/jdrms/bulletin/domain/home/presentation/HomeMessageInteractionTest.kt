@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.jdrms.bulletin.domain.home.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.application.ContactSellerFailure
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomeMessageInteractionTest {
    @Test
    fun quick_reply_edits_only_and_explicit_send_validates_and_trims() = interactionTest { f ->
        f.vm.onDraftChanged("ignored")
        assertEquals("", f.interaction.tray.draft)
        f.vm.openTray()
        f.vm.onQuickReplySelected("  Is this still available?  ")
        assertTrue(f.sends.isEmpty())
        f.vm.saveCurrentCard()
        f.vm.passCurrentCard()
        assertEquals(CardState.Open, f.interaction.card)
        assertTrue(f.saves.isEmpty())
        f.vm.sendMessage()
        assertEquals(listOf(Triple("viewer", HomeItemId("a"), "Is this still available?")), f.sends)
        assertTrue(f.interaction.tray.sent)
    }

    @Test
    fun blank_and_overlength_are_inline_errors_and_do_not_start_send() = interactionTest { f ->
        f.vm.openTray()
        listOf(
            "  " to HomeInteractionError.BLANK_MESSAGE,
            "a".repeat(201) to HomeInteractionError.MESSAGE_TOO_LONG
        ).forEach {
                (draft, error) ->
            f.vm.onDraftChanged(draft)
            f.vm.sendMessage()
            assertEquals(error, f.interaction.tray.error)
            assertEquals(draft, f.interaction.tray.draft)
            assertEquals(CardState.Open, f.interaction.card)
        }
        assertTrue(f.sends.isEmpty())
        f.vm.onDraftChanged("a".repeat(200))
        f.vm.sendMessage()
        assertEquals(1, f.sends.size)
    }

    @Test
    fun success_exposes_receipt_before_departure_and_blocks_repeated_send() = interactionTest { f ->
        val result = CompletableDeferred<Result<Unit>>()
        f.sendResult = { result.await() }
        f.vm.openTray()
        f.vm.onDraftChanged("hello")
        f.vm.sendMessage()
        f.vm.sendMessage()
        f.vm.closeTray()
        f.vm.onDraftChanged("ignored")
        f.vm.saveCurrentCard()
        f.vm.passCurrentCard()
        assertEquals(CardState.Sending, f.interaction.card)
        assertEquals(1, f.sends.size)
        result.complete(Result.Success(Unit))
        advanceUntilIdle()
        assertTrue(f.interaction.tray.sent)
        assertEquals(CardState.Sending, f.interaction.card)
        val receiptId = requireNotNull(f.interaction.tray.sentReceiptId)
        assertNull(f.interaction.departure)
        f.vm.onExitCompleted(receiptId)
        f.vm.onSentReceiptCompleted(receiptId + 1)
        assertEquals(0, f.vm.uiState.value.currentIndex)
        f.vm.onSentReceiptCompleted(receiptId)
        assertEquals(CardState.Leaving, f.interaction.card)
        f.exit()
        assertEquals(HomeItemId("b"), f.interaction.listingId)
        assertFalse(f.interaction.tray.sent)
        assertEquals("", f.interaction.tray.draft)
    }

    @Test
    fun typed_failures_restore_exact_draft_and_keep_card_without_auto_retry() = interactionTest { f ->
        val failures = listOf(
            ContactSellerFailure.OwnListing() to HomeInteractionError.OWN_LISTING,
            ContactSellerFailure.ListingUnavailable() to HomeInteractionError.LISTING_UNAVAILABLE,
            ContactSellerFailure.NotSignedIn() to HomeInteractionError.NOT_SIGNED_IN,
            ContactSellerFailure.Network() to HomeInteractionError.NETWORK,
            ContactSellerFailure.Unknown() to HomeInteractionError.UNKNOWN
        )
        f.vm.openTray()
        failures.forEach { (failure, error) ->
            f.sendResult = { Result.Error(failure) }
            f.vm.onDraftChanged("  original draft  ")
            f.vm.sendMessage()
            advanceUntilIdle()
            assertEquals(CardState.Open, f.interaction.card)
            assertEquals("  original draft  ", f.interaction.tray.draft)
            assertEquals(error, f.interaction.tray.error)
            assertEquals(0, f.vm.uiState.value.currentIndex)
            assertTrue(f.interaction.pendingEffects.isEmpty())
        }
        assertEquals(failures.size, f.sends.size)
    }

    @Test
    fun send_cancellation_restores_open_draft_without_inline_error_or_effect() = interactionTest { f ->
        f.sendResult = { throw CancellationException("cancelled") }
        f.vm.openTray()
        f.vm.onDraftChanged("  original  ")
        f.vm.sendMessage()
        advanceUntilIdle()
        assertEquals(CardState.Open, f.interaction.card)
        assertEquals(HomeTrayState(draft = "  original  "), f.interaction.tray)
        assertTrue(f.interaction.pendingEffects.isEmpty())
    }

    @Test
    fun cancellation_returned_by_port_is_preserved_through_use_case_failure_wrapping() = interactionTest { f ->
        f.sendResult = { Result.Error(CancellationException("cancelled")) }
        f.vm.openTray()
        f.vm.onDraftChanged("draft")
        f.vm.sendMessage()
        advanceUntilIdle()
        assertEquals(CardState.Open, f.interaction.card)
        assertEquals(HomeTrayState(draft = "draft"), f.interaction.tray)
        assertTrue(f.interaction.pendingEffects.isEmpty())
    }

    @Test
    fun unexpected_send_exception_is_sanitized_and_never_exposed() = interactionTest { f ->
        f.sendResult = { error("private transport information") }
        f.vm.openTray()
        f.vm.onDraftChanged("message")
        f.vm.sendMessage()
        assertEquals(HomeInteractionError.UNKNOWN, f.interaction.tray.error)
        assertFalse(f.interaction.toString().contains("private transport"))
        assertEquals("message", f.interaction.tray.draft)
    }
}
