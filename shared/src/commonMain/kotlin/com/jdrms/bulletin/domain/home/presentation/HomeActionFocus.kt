package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import com.jdrms.bulletin.domain.home.domain.model.CardAction

/** Remembers a keyboard opener across temporary disabled controls and tray focus. */
internal class HomeActionFocus {
    private val requesters = CardAction.entries.associateWith { FocusRequester() }
    private var focused: CardAction? = null
    var restoreAction by mutableStateOf<CardAction?>(null)
        private set

    fun prepare(action: CardAction) {
        restoreAction = action.takeIf { focused == it || it == CardAction.Message }
    }

    @Composable
    fun modifier(action: CardAction, enabled: Boolean): Modifier {
        var accessibilityTarget by remember { mutableStateOf<String?>(null) }
        HomePlatformAccessibilityFocus(accessibilityTarget)
        LaunchedEffect(enabled, restoreAction) {
            if (enabled && restoreAction == action) {
                // Closing tray motion can outlive Idle; restore only once this control re-enables.
                withFrameNanos { }
                requesters.getValue(action).requestFocus()
                restoreAction = null
                accessibilityTarget = tag(action)
            } else if (!enabled) {
                accessibilityTarget = null
            }
        }
        return Modifier.homeFocusTarget(tag(action)).focusRequester(requesters.getValue(action)).onFocusChanged {
            if (it.isFocused) focused = action else if (focused == action) focused = null
        }
    }

    private fun tag(action: CardAction): String = "home_action_${action.name}"
}
