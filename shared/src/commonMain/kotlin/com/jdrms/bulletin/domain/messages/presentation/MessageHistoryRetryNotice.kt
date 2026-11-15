package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

@Composable
internal fun MessageHistoryRetryNotice(state: MessagesUiState, onRetry: (MessageHistoryRequest) -> Unit) {
    listOfNotNull(state.failedHistoryRequest, state.failedOlderHistoryRequest).forEach { request ->
        val loading = when (request) {
            is MessageHistoryRequest.Thread -> state.isLoadingMessages
            is MessageHistoryRequest.Older -> state.isLoadingOlder
        }
        val message = when (request) {
            is MessageHistoryRequest.Thread -> "Unable to load conversation messages. Please try again."
            is MessageHistoryRequest.Older -> "Unable to load older messages. Please try again."
        }
        Row(Modifier.padding(horizontal = BulletinSpacing.gutter), verticalAlignment = Alignment.CenterVertically) {
            Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
            BulletinButton(
                onClick = { onRetry(request) },
                enabled = !loading,
                variant = BulletinButtonVariant.Text
            ) { Text(if (loading) "Retrying..." else "Retry") }
        }
    }
}
