package com.jdrms.bulletin.domain.home.presentation

typealias CardState = com.jdrms.bulletin.domain.home.domain.model.CardState

enum class CardEvent { DRAG_X, DRAG_Y, OPEN, COMMIT, CANCEL, CLOSE, SEND, SENT, FAIL, DONE }

/** A rejection retains the input state and cannot be mistaken for an accepted transition. */
sealed interface CardTransition {
    data class Accepted(val next: CardState) : CardTransition

    data class Rejected(val current: CardState) : CardTransition
}

/** Pure transition policy. The caller owns state and applies only accepted transitions. */
object CardStateMachine {
    private val transitions = mapOf(
        CardState.Idle to mapOf(
            CardEvent.DRAG_X to CardState.Swiping,
            CardEvent.DRAG_Y to CardState.Peeking,
            CardEvent.OPEN to CardState.Open,
            CardEvent.COMMIT to CardState.Leaving,
        ),
        CardState.Swiping to mapOf(CardEvent.COMMIT to CardState.Leaving, CardEvent.CANCEL to CardState.Idle),
        CardState.Peeking to mapOf(CardEvent.OPEN to CardState.Open, CardEvent.CANCEL to CardState.Idle),
        CardState.Open to mapOf(CardEvent.CLOSE to CardState.Idle, CardEvent.SEND to CardState.Sending),
        CardState.Sending to mapOf(CardEvent.SENT to CardState.Leaving, CardEvent.FAIL to CardState.Open),
        CardState.Leaving to mapOf(CardEvent.DONE to CardState.Idle),
    )

    fun transition(state: CardState, event: CardEvent): CardTransition {
        val next = transitions[state]?.get(event) ?: return CardTransition.Rejected(state)
        return CardTransition.Accepted(next)
    }

    /** Matches the reference pointer-start guard; an ongoing drag retains its already locked axis. */
    fun acceptsPointerInput(state: CardState): Boolean = state == CardState.Idle || state == CardState.Open
}
