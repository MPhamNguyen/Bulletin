package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

@Composable
internal fun homeTrayDismissal(state: CardState, actions: HomeTrayActions): Modifier {
    val open = state == CardState.Open
    HomePlatformTrayBackHandler(open, actions::closeTray)
    return Modifier.onPreviewKeyEvent {
        if (open && it.type == KeyEventType.KeyDown && it.key == Key.Escape) {
            actions.closeTray()
            true
        } else {
            false
        }
    }
}

@Composable
internal expect fun HomePlatformTrayBackHandler(enabled: Boolean, onClose: () -> Unit)
