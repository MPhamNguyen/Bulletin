package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import bulletin.shared.generated.resources.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/** UI-only event delivery. IDs come from the existing receipt, departure and confirmed-Save effects. */
internal class HomeAnnouncements {
    val messages = Channel<String>(Channel.UNLIMITED)
    var sendAttempt by mutableIntStateOf(0)
        private set
    fun beginSend() { sendAttempt++ }
    private val delivered = mutableSetOf<String>()
    fun once(key: String, message: String) {
        if (delivered.add(key)) messages.trySend(message)
    }
}

internal val LocalHomeAnnouncements = staticCompositionLocalOf { HomeAnnouncements() }

@Composable
internal fun HomeAnnouncementRegion(announcements: HomeAnnouncements, state: HomeInteractionState) {
    var message by remember { mutableStateOf("") }
    HomeStateAnnouncements(announcements, state)
    LaunchedEffect(announcements) {
        for (next in announcements.messages) {
            message = ""
            delay(120)
            message = next
            // Android batches semantics events; preserve each queued update across a batch.
            delay(300)
        }
    }
    Box(
        Modifier.size(1.dp).semantics {
            liveRegion = LiveRegionMode.Polite
            contentDescription = message
            testTag = "home_announcement"
        }
    )
}

@Composable
internal fun HomeStateAnnouncements(announcements: HomeAnnouncements, state: HomeInteractionState) {
    var guestAnnounced by rememberSaveable { mutableStateOf(false) }
    val guest = stringResource(Res.string.home_signed_out)
    val failure = stringResource(Res.string.home_message_failed)
    LaunchedEffect(state.accountNotice) {
        if (state.accountNotice == HomeInteractionError.NOT_SIGNED_IN && !guestAnnounced) {
            guestAnnounced = true
            announcements.messages.trySend(guest)
        }
        if (state.accountNotice == null) guestAnnounced = false
    }
    LaunchedEffect(state.tray.error, announcements.sendAttempt) {
        if (state.tray.error != null && state.tray.error != HomeInteractionError.DECISION_STORAGE) {
            announcements.messages.trySend(failure)
        }
    }
}
