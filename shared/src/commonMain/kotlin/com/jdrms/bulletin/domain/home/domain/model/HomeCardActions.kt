package com.jdrms.bulletin.domain.home.domain.model

enum class CardState { Idle, Swiping, Peeking, Open, Sending, Leaving }
enum class CardAction { Pass, Message, Save }
enum class UnavailableReason { CardBusy, GuestUser, SavingNotConfigured, MessagingNotConfigured, TrayOpen, NoListing }

sealed interface ActionAvailability {
    data object Available : ActionAvailability
    data class Unavailable(val reason: UnavailableReason) : ActionAvailability
}

/** Capabilities supplied by the application; no session or adapter types cross this boundary. */
data class HomeActionContext(
    val hasListing: Boolean,
    val signedIn: Boolean,
    val canSave: Boolean,
    val canMessage: Boolean
)

class AvailableActions internal constructor(private val byAction: Map<CardAction, ActionAvailability>) {
    operator fun get(action: CardAction): ActionAvailability = byAction.getValue(action)
    fun allows(action: CardAction): Boolean = this[action] == ActionAvailability.Available
}

/** Swiping and Peeking permit the matching release command, but never a competing action. */
fun CardState.availableActions(context: HomeActionContext): AvailableActions = AvailableActions(
    CardAction.entries.associateWith { action ->
        val reason = when {
            !context.hasListing -> UnavailableReason.NoListing
            phaseRejection(action) != null -> phaseRejection(action)
            action != CardAction.Pass && !context.signedIn -> UnavailableReason.GuestUser
            action == CardAction.Save && !context.canSave -> UnavailableReason.SavingNotConfigured
            action == CardAction.Message && !context.canMessage -> UnavailableReason.MessagingNotConfigured
            else -> null
        }
        reason?.let(ActionAvailability::Unavailable) ?: ActionAvailability.Available
    }
)

private fun CardState.phaseRejection(action: CardAction): UnavailableReason? = when (this) {
    CardState.Idle -> null
    CardState.Swiping -> if (action == CardAction.Message) UnavailableReason.CardBusy else null
    CardState.Peeking -> if (action == CardAction.Message) null else UnavailableReason.CardBusy
    CardState.Open -> UnavailableReason.TrayOpen
    CardState.Sending, CardState.Leaving -> UnavailableReason.CardBusy
}
