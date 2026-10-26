package com.jdrms.bulletin.domain.home.presentation

import com.jdrms.bulletin.domain.home.application.RecordSwipeDecision
import com.jdrms.bulletin.domain.home.application.SaveListingPort
import com.jdrms.bulletin.domain.home.application.SendQuickMessage
import com.jdrms.bulletin.domain.home.domain.model.CardAction
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import com.jdrms.bulletin.domain.home.domain.repository.SwipeDecisionRepository
import kotlinx.coroutines.flow.update

/** Card commands delegate legality to Feature 5; save and send own their asynchronous lifecycles. */
internal class HomeCardInteraction(
    decisions: SwipeDecisionRepository,
    record: RecordSwipeDecision,
    send: SendQuickMessage?,
    save: SaveListingPort?
) : HomeCardActions {
    val store = HomeInteractionStore(canSave = save != null, canMessage = send != null)
    private val saves = HomeSaveInteraction(store, record, decisions, save)
    private val messages = HomeMessageInteraction(store, send)

    override fun saveCurrentCard() = saves.saveCurrent()
    override fun passCurrentCard() = saves.passCurrent()
    override fun openTray() {
        if (store.interaction.availableActions.allows(CardAction.Message)) store.transition(CardEvent.OPEN)
    }
    override fun closeTray() {
        store.transition(CardEvent.CLOSE)
    }
    override fun onDraftChanged(text: String) = messages.editDraft(text)
    override fun onQuickReplySelected(text: String) = messages.editDraft(text)
    override fun sendMessage() = messages.send()
    override fun retrySave(listingId: HomeItemId) = saves.retry(listingId)
    override fun dismissSaveRetry(listingId: HomeItemId) = saves.dismiss(listingId)
    override fun onEffectConsumed(id: Long) {
        store.edit { it.copy(pendingEffects = it.pendingEffects.filterNot { effect -> effect.id == id }) }
    }
    override fun onGestureEvent(event: CardEvent) {
        if (store.interaction.listingId == null) return
        when (event) {
            CardEvent.DRAG_X, CardEvent.CANCEL -> store.transition(event)
            CardEvent.DRAG_Y -> {
                if (store.interaction.availableActions.allows(CardAction.Message)) store.transition(event)
            }
            else -> Unit
        }
    }
    override fun onSentReceiptCompleted(receiptId: Long) {
        if (store.interaction.tray.sentReceiptId == receiptId && store.transition(CardEvent.SENT)) {
            store.requestDeparture(HomeDepartureKind.SENT)
        }
    }
    override fun onExitCompleted(departureId: Long) {
        if (store.interaction.departure?.id != departureId || !store.transition(CardEvent.DONE)) return
        store.state.update { it.copy(currentIndex = it.currentIndex + 1, prefetchEnabled = true) }
        store.reset()
    }

    fun reset() {
        saves.reset()
        store.reset(clearEffects = true)
    }
}
