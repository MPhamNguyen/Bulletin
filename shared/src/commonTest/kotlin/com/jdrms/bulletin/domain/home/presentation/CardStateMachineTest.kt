package com.jdrms.bulletin.domain.home.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CardStateMachineTest {
    private val allowed = mapOf(
        (CardState.Idle to CardEvent.DRAG_X) to CardState.Swiping,
        (CardState.Idle to CardEvent.DRAG_Y) to CardState.Peeking,
        (CardState.Idle to CardEvent.OPEN) to CardState.Open,
        (CardState.Idle to CardEvent.COMMIT) to CardState.Leaving,
        (CardState.Swiping to CardEvent.COMMIT) to CardState.Leaving,
        (CardState.Swiping to CardEvent.CANCEL) to CardState.Idle,
        (CardState.Peeking to CardEvent.OPEN) to CardState.Open,
        (CardState.Peeking to CardEvent.CANCEL) to CardState.Idle,
        (CardState.Open to CardEvent.CLOSE) to CardState.Idle,
        (CardState.Open to CardEvent.SEND) to CardState.Sending,
        (CardState.Sending to CardEvent.SENT) to CardState.Leaving,
        (CardState.Sending to CardEvent.FAIL) to CardState.Open,
        (CardState.Leaving to CardEvent.DONE) to CardState.Idle,
    )

    @Test
    fun every_allowed_transition_returns_its_next_state() {
        allowed.forEach { (input, next) ->
            val (state, event) = input
            assertEquals(CardTransition.Accepted(next), CardStateMachine.transition(state, event), "$state + $event")
        }
    }

    @Test
    fun full_state_event_cross_product_matches_the_allowed_table_and_rejections_retain_state() {
        var rejectedCount = 0
        CardState.entries.forEach { state ->
            CardEvent.entries.forEach { event ->
                val next = allowed[state to event]
                val result = CardStateMachine.transition(state, event)
                if (next == null) {
                    assertEquals(state, assertIs<CardTransition.Rejected>(result).current, "$state + $event")
                    rejectedCount++
                } else {
                    assertEquals(CardTransition.Accepted(next), result, "$state + $event")
                }
            }
        }
        assertEquals(47, rejectedCount)
    }

    @Test
    fun sending_is_unreachable_from_idle_without_passing_through_open() {
        val reachableWithoutOpen = mutableSetOf(CardState.Idle)
        val pending = ArrayDeque<CardState>().apply { add(CardState.Idle) }
        while (pending.isNotEmpty()) {
            val state = pending.removeFirst()
            CardEvent.entries.forEach { event ->
                val result = CardStateMachine.transition(state, event)
                if (result is CardTransition.Accepted && result.next != CardState.Open &&
                    reachableWithoutOpen.add(result.next)
                ) {
                    pending.add(result.next)
                }
            }
        }
        assertFalse(CardState.Sending in reachableWithoutOpen)
        assertEquals(
            CardState.entries.toSet() - setOf(CardState.Open, CardState.Sending),
            reachableWithoutOpen,
        )
        CardState.entries.forEach { state ->
            CardEvent.entries.forEach { event ->
                if (CardStateMachine.transition(state, event) == CardTransition.Accepted(CardState.Sending)) {
                    assertEquals(CardState.Open to CardEvent.SEND, state to event)
                }
            }
        }
    }

    @Test
    fun failed_send_returns_to_open_for_retry_then_success_can_leave_and_finish() {
        var state = CardState.Idle
        val steps = listOf(
            CardEvent.DRAG_Y to CardState.Peeking,
            CardEvent.OPEN to CardState.Open,
            CardEvent.SEND to CardState.Sending,
            CardEvent.FAIL to CardState.Open,
            CardEvent.SEND to CardState.Sending,
            CardEvent.SENT to CardState.Leaving,
            CardEvent.DONE to CardState.Idle,
        )
        steps.forEach { (event, expected) ->
            state = assertIs<CardTransition.Accepted>(CardStateMachine.transition(state, event)).next
            assertEquals(expected, state)
        }
    }

    @Test
    fun event_vocabulary_has_no_flick() {
        assertEquals(
            setOf("DRAG_X", "DRAG_Y", "OPEN", "COMMIT", "CANCEL", "CLOSE", "SEND", "SENT", "FAIL", "DONE"),
            CardEvent.entries.map { it.name }.toSet(),
        )
    }

    @Test
    fun pointer_input_is_accepted_only_in_idle_and_open() {
        CardState.entries.forEach { state ->
            if (state == CardState.Idle || state == CardState.Open) {
                assertTrue(CardStateMachine.acceptsPointerInput(state), state.name)
            } else {
                assertFalse(CardStateMachine.acceptsPointerInput(state), state.name)
            }
        }
    }
}
