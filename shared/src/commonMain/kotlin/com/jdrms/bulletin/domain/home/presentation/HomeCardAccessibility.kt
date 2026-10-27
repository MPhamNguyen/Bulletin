package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.*
import bulletin.shared.generated.resources.*
import com.jdrms.bulletin.domain.home.domain.model.CardAction
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedItem
import org.jetbrains.compose.resources.stringResource

internal const val HOME_CARD_FOCUS_TAG = "home_current_card"
internal const val HOME_COMPOSER_FOCUS_TAG = "home_composer"

@Composable
internal fun homeCardAccessibility(
    item: HomeFeedItem,
    interaction: HomeInteractionState,
    actions: HomeCardActions,
    focus: FocusRequester
): Modifier {
    val latest by rememberUpdatedState(interaction)
    val available = homeCardAccessibleActions(interaction, actions)
    val hint = available.joinToString { it.label }
    val description = stringResource(Res.string.home_card_description, item.title, item.price.formatted, hint)
    return Modifier.homeFocusTarget(HOME_CARD_FOCUS_TAG).focusRequester(focus).semantics {
        contentDescription = description
        customActions = available
    }.onKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) false else homeCardArrow(event.key, latest, actions)
    }.focusable(enabled = interaction.card == CardState.Idle)
}

@Composable
private fun homeCardAccessibleActions(
    state: HomeInteractionState,
    actions: HomeCardActions
): List<CustomAccessibilityAction> {
    val save = stringResource(Res.string.home_action_save)
    val pass = stringResource(Res.string.home_action_pass)
    val message = stringResource(Res.string.home_action_message)
    if (state.card != CardState.Idle) return emptyList()
    return buildList {
        if (state.availableActions.allows(CardAction.Save)) {
            add(
                CustomAccessibilityAction(save) {
                    actions.saveCurrentCard()
                    true
                }
            )
        }
        if (state.availableActions.allows(CardAction.Pass)) {
            add(
                CustomAccessibilityAction(pass) {
                    actions.passCurrentCard()
                    true
                }
            )
        }
        if (state.availableActions.allows(CardAction.Message)) {
            add(
                CustomAccessibilityAction(message) {
                    actions.openTray()
                    true
                }
            )
        }
    }
}

internal fun homeCardArrow(key: Key, state: HomeInteractionState, actions: HomeCardActions): Boolean {
    if (state.card != CardState.Idle) return false
    return when {
        key == Key.DirectionRight && state.availableActions.allows(CardAction.Save) -> {
            actions.saveCurrentCard()
            true
        }
        key == Key.DirectionLeft && state.availableActions.allows(CardAction.Pass) -> {
            actions.passCurrentCard()
            true
        }
        key == Key.DirectionUp && state.availableActions.allows(CardAction.Message) -> {
            actions.openTray()
            true
        }
        else -> false
    }
}

internal fun Modifier.homeFocusTarget(tag: String): Modifier = semantics {
    testTag = tag
    testTagsAsResourceId = true
}
