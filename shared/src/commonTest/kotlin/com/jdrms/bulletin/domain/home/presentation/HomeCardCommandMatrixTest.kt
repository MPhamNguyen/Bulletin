@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.jdrms.bulletin.domain.home.presentation

import com.jdrms.bulletin.core.common.Result
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeCardCommandMatrixTest {
    @Test
    fun every_card_command_uses_machine_acceptance_in_every_card_state() = interactionTest { f ->
        val commands: List<Pair<CardEvent, (HomeViewModel) -> Unit>> = listOf(
            CardEvent.COMMIT to { it.saveCurrentCard() },
            CardEvent.COMMIT to { it.passCurrentCard() },
            CardEvent.OPEN to { it.openTray() },
            CardEvent.CLOSE to { it.closeTray() },
            CardEvent.SEND to { it.sendMessage() },
            CardEvent.DRAG_X to { it.onGestureEvent(CardEvent.DRAG_X) },
            CardEvent.DRAG_Y to { it.onGestureEvent(CardEvent.DRAG_Y) },
            CardEvent.CANCEL to { it.onGestureEvent(CardEvent.CANCEL) },
            CardEvent.DONE to { it.onExitCompleted(f.interaction.departure?.id ?: -1) }
        )
        CardState.entries.forEach { state ->
            commands.forEach { (event, command) ->
                f.decisions.clearAllDecisions("viewer")
                f.vm.refresh()
                advanceUntilIdle()
                prepare(f, state)
                val saves = f.saves.size
                val sends = f.sends.size
                val transition = CardStateMachine.transition(state, event)
                command(f.vm)
                val expected = (transition as? CardTransition.Accepted)?.next ?: state
                assertEquals(expected, f.interaction.card, "$state / $event")
                if (transition is CardTransition.Rejected) {
                    assertEquals(saves, f.saves.size)
                    assertEquals(sends, f.sends.size)
                }
            }
        }
    }
}

private fun prepare(f: HomeInteractionFixture, state: CardState) {
    f.sendResult = { CompletableDeferred<Result<Unit>>().await() }
    when (state) {
        CardState.Idle -> Unit
        CardState.Swiping -> f.vm.onGestureEvent(CardEvent.DRAG_X)
        CardState.Peeking -> f.vm.onGestureEvent(CardEvent.DRAG_Y)
        CardState.Open, CardState.Sending -> {
            f.vm.openTray()
            f.vm.onDraftChanged("draft")
            if (state == CardState.Sending) f.vm.sendMessage()
        }
        CardState.Leaving -> f.vm.passCurrentCard()
    }
}
