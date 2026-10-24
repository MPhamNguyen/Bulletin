package com.jdrms.bulletin.domain.home.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.application.SendQuickMessage
import com.jdrms.bulletin.domain.home.domain.model.SellerMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

internal class HomeMessageInteraction(private val store: HomeInteractionStore, private val send: SendQuickMessage?) {
    fun editDraft(text: String) {
        if (store.interaction.card == CardState.Open && !store.interaction.tray.sent) {
            store.edit { it.copy(tray = HomeTrayState(draft = text)) }
        }
    }

    /** One explicit attempt only; ambiguous failures may have delivered a message. Never auto-retry. */
    fun send() {
        if (!store.interaction.messageAvailable || store.interaction.tray.sent ||
            CardStateMachine.transition(store.interaction.card, CardEvent.SEND) is CardTransition.Rejected
        ) {
            return
        }
        val capture = store.capture() ?: return
        val draft = store.interaction.tray.draft
        when (val validation = SellerMessage.create(draft)) {
            is Result.Error -> {
                store.edit { it.copy(tray = it.tray.copy(error = validationError(validation.exception))) }
                return
            }
            is Result.Success -> Unit
        }
        if (!store.transition(CardEvent.SEND)) return
        store.edit { it.copy(tray = HomeTrayState()) }
        store.sendJob = store.scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                val result =
                    interactionAttempt { requireNotNull(send).invoke(capture.viewer, capture.listingId, draft) }
                if (store.matches(capture)) finish(result, draft)
            } catch (cancellation: CancellationException) {
                if (store.matches(capture)) recover(draft, null)
                throw cancellation
            }
        }
    }

    private fun finish(result: Result<Unit>, draft: String) {
        when (result) {
            is Result.Success -> {
                // Keep Sending until the UI acknowledges the receipt; no VM-owned dwell timer.
                store.edit { it.copy(tray = HomeTrayState(sentReceiptId = store.nextId())) }
            }
            is Result.Error -> recover(draft, messageError(result.exception))
        }
    }

    private fun recover(draft: String, error: HomeInteractionError?) {
        if (store.transition(CardEvent.FAIL)) store.edit { it.copy(tray = HomeTrayState(draft, error)) }
    }
}
