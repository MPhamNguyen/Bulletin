package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinSpacing

@Composable
internal fun HomeTrayPeek(
    state: HomeInteractionState,
    actions: HomeTrayActions,
    progress: () -> Float,
    durationScale: Float,
    modifier: Modifier = Modifier,
    composerFocus: FocusRequester = FocusRequester()
) {
    val active = state.card == CardState.Open && state.messageAvailable && !state.tray.sent
    Column(
        modifier.then(
            if (state.isTrayOpen) {
                Modifier
            } else {
                Modifier.clearAndSetSemantics {
                }
            }
        ).fillMaxWidth().height(HOME_TRAY_LIFT_DP.dp).graphicsLayer {
            alpha = homeTrayAlpha(progress())
            translationY = (1f - progress()) * 28.dp.toPx()
        }.clipToBounds().background(
            MaterialTheme.colorScheme.surfaceContainer,
            MaterialTheme.shapes.large
        ).padding(BulletinSpacing.s),
        verticalArrangement = Arrangement.spacedBy(BulletinSpacing.xs, Alignment.Bottom)
    ) {
        // Measure the composer first. Optional header content yields space on short IME viewports.
        Column(Modifier.weight(1f, fill = false)) {
            HomeTrayHeader(state, active, actions, durationScale)
        }
        HomeTrayComposer(state, actions, composerFocus)
    }
}

@Composable
private fun HomeQuickReplyRow(active: Boolean, onSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.heightIn(min = 48.dp).fillMaxWidth().horizontalScroll(rememberScrollState(), enabled = active),
        Arrangement.spacedBy(BulletinSpacing.s)
    ) {
        homeQuickReplies().forEach { reply ->
            SuggestionChip(modifier = Modifier.heightIn(min = 48.dp), onClick = {
                onSelected(reply)
            }, enabled = active, label = { Text(reply) })
        }
    }
}

@Composable
private fun HomeTrayHeader(
    state: HomeInteractionState,
    active: Boolean,
    actions: HomeTrayActions,
    durationScale: Float
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (state.card == CardState.Sending) {
            Box(Modifier.weight(1f)) { HomeTrayReceipt(state.tray.sent, durationScale) }
        } else {
            HomeQuickReplyRow(active, actions::onQuickReplySelected, Modifier.weight(1f))
        }
    }
    state.tray.error?.let {
        Text(
            homeInteractionErrorText(it),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
