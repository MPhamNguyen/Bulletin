package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import bulletin.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HomeTrayPeek(
    state: HomeInteractionState,
    actions: HomeTrayActions,
    progress: () -> Float,
    durationScale: Float,
    modifier: Modifier = Modifier
) {
    val active = state.card == CardState.Open && state.messageAvailable && !state.tray.sent
    Column(
        modifier.fillMaxWidth().height(HOME_TRAY_LIFT_DP.dp).graphicsLayer {
            alpha = homeTrayAlpha(progress())
            translationY = (1f - progress()) * 28.dp.toPx()
        }.background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.large).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = actions::closeTray, enabled = active, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, stringResource(Res.string.home_close_tray))
            }
        }
        if (state.card == CardState.Sending) {
            HomeTrayReceipt(state.tray.sent, durationScale)
        } else {
            HomeQuickReplyRow(active, actions::onQuickReplySelected)
        }
        state.tray.error?.let {
            Text(
                homeInteractionErrorText(it),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        HomeTrayComposer(state, actions)
    }
}

@Composable
private fun HomeQuickReplyRow(active: Boolean, onSelected: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState(), enabled = active), Arrangement.spacedBy(8.dp)) {
        homeQuickReplies().forEach { reply ->
            SuggestionChip(onClick = { onSelected(reply) }, enabled = active, label = { Text(reply) })
        }
    }
}
