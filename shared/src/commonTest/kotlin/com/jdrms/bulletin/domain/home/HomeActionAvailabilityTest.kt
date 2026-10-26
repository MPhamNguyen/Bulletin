package com.jdrms.bulletin.domain.home

import com.jdrms.bulletin.domain.home.domain.model.*
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeActionAvailabilityTest {
    @Test
    fun every_state_action_session_and_capability_combination() {
        val contexts = (0..7).map { mask ->
            HomeActionContext(true, mask and 1 != 0, mask and 2 != 0, mask and 4 != 0)
        }
        CardState.entries.forEach { state ->
            contexts.forEach { context ->
                val actions = state.availableActions(context)
                CardAction.entries.forEach { action ->
                    assertEquals(
                        expected(state, action, context),
                        actions.allows(action),
                        "$state / $action / $context"
                    )
                }
            }
        }
    }

    private fun expected(state: CardState, action: CardAction, context: HomeActionContext): Boolean {
        val phases = when (action) {
            CardAction.Pass, CardAction.Save -> listOf(CardState.Idle, CardState.Swiping)
            CardAction.Message -> listOf(CardState.Idle, CardState.Peeking)
        }
        val capability = when (action) {
            CardAction.Pass -> true
            CardAction.Save -> context.signedIn && context.canSave
            CardAction.Message -> context.signedIn && context.canMessage
        }
        return state in phases && capability
    }

    @Test
    fun guest_and_missing_configuration_have_explicit_reasons() {
        val guest = CardState.Idle.availableActions(HomeActionContext(true, false, true, true))
        assertEquals(ActionAvailability.Unavailable(UnavailableReason.GuestUser), guest[CardAction.Save])
        val unconfigured = CardState.Idle.availableActions(HomeActionContext(true, true, false, false))
        assertEquals(
            ActionAvailability.Unavailable(UnavailableReason.SavingNotConfigured),
            unconfigured[CardAction.Save]
        )
        assertEquals(
            ActionAvailability.Unavailable(UnavailableReason.MessagingNotConfigured),
            unconfigured[CardAction.Message]
        )
    }

    @Test
    fun no_listing_disables_every_action_in_every_phase() {
        CardState.entries.forEach { state ->
            val actions = state.availableActions(HomeActionContext(false, true, true, true))
            CardAction.entries.forEach {
                assertEquals(ActionAvailability.Unavailable(UnavailableReason.NoListing), actions[it])
            }
        }
    }
}
