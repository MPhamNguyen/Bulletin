package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager

/** Input focus and accessibility focus are separate requests. Never steal focus after a failed send. */
@Composable
internal fun HomeTrayFocus(
    state: CardState,
    cardFocus: FocusRequester,
    composerFocus: FocusRequester,
    restoreCardFocus: Boolean = true
) {
    var wasOpen by remember { mutableStateOf(false) }
    val manager = LocalFocusManager.current
    var target by remember { mutableStateOf<String?>(null) }
    val opening = state == CardState.Open && !wasOpen
    val closing = state == CardState.Idle && wasOpen
    HomePlatformAccessibilityFocus(target)
    LaunchedEffect(state) {
        if (opening || closing && restoreCardFocus) {
            withFrameNanos { }
            if (closing) manager.clearFocus(force = true)
            (if (opening) composerFocus else cardFocus).requestFocus()
            target = if (opening) HOME_COMPOSER_FOCUS_TAG else HOME_CARD_FOCUS_TAG
        }
        if (state == CardState.Open) wasOpen = true
        if (state == CardState.Idle) wasOpen = false
    }
}

@Composable
internal expect fun HomePlatformAccessibilityFocus(targetTag: String?)
