package com.jdrms.bulletin.domain.home.presentation

import com.jdrms.bulletin.domain.home.domain.model.HomeItemId

enum class HomeDepartureKind { SAVE, PASS, SENT }

data class HomeDeparture(val id: Long, val listingId: HomeItemId, val kind: HomeDepartureKind)

enum class HomeInteractionError {
    BLANK_MESSAGE, MESSAGE_TOO_LONG, NOT_SIGNED_IN, LISTING_UNAVAILABLE, OWN_LISTING,
    NETWORK, UNKNOWN, DECISION_STORAGE
}

sealed interface HomeEffect {
    val id: Long
    data class SaveRetryOffered(override val id: Long, val listingId: HomeItemId) : HomeEffect
}

data class HomeTrayState(
    val draft: String = "",
    val error: HomeInteractionError? = null,
    val sentReceiptId: Long? = null
) {
    val sent: Boolean get() = sentReceiptId != null
}

data class HomeInteractionState(
    val listingId: HomeItemId? = null,
    val card: CardState = CardState.Idle,
    val tray: HomeTrayState = HomeTrayState(),
    val departure: HomeDeparture? = null,
    val pendingEffects: List<HomeEffect> = emptyList(),
    val accountNotice: HomeInteractionError? = HomeInteractionError.NOT_SIGNED_IN,
    val saveAvailable: Boolean = false,
    val messageAvailable: Boolean = false
) {
    val isTrayOpen: Boolean get() = card == CardState.Open || card == CardState.Sending
}

interface HomeSaveActions {
    fun saveCurrentCard()
    fun retrySave(listingId: HomeItemId)
    fun dismissSaveRetry(listingId: HomeItemId)
}

interface HomeTrayActions {
    fun openTray()
    fun closeTray()
    fun onDraftChanged(text: String)
    fun onQuickReplySelected(text: String)
    fun sendMessage()
}

interface HomeCardActions : HomeSaveActions, HomeTrayActions {
    fun passCurrentCard()
    fun onEffectConsumed(id: Long)
    fun onGestureEvent(event: CardEvent)
    fun onSentReceiptCompleted(receiptId: Long)
    fun onExitCompleted(departureId: Long)
}
