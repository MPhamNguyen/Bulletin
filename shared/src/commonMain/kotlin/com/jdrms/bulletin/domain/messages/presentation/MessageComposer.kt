package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinIconButtonDefaults
import com.jdrms.bulletin.core.designsystem.BulletinSpacing

@Composable
internal fun MessageComposer(state: MessagesUiState, viewModel: MessagesViewModel) {
    val isEditing = state.editState != MessageEditState.Composing
    val isBusy = state.isSending || state.editState is MessageEditState.Saving || !state.canSendToCounterpart()
    Column(Modifier.fillMaxWidth().padding(horizontal = BulletinSpacing.gutter, vertical = BulletinSpacing.s)) {
        if (isEditing) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Editing message", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = viewModel.cancelEditing, enabled = !isBusy) { Text("Cancel") }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s)
        ) {
            MessageInputField(
                value = state.messageInput,
                onValueChange = viewModel::onMessageInputChanged,
                placeholder = if (isEditing) "Edit your message..." else "Type a message...",
                enabled = !isBusy,
                isError = state.editError != null,
                modifier = Modifier.weight(1f)
            )
            FilledIconButton(
                onClick = viewModel::sendCurrentMessage,
                enabled = !isBusy && !state.isLoadingMessages && state.messageInput.isNotBlank(),
                modifier = Modifier.size(BulletinSpacing.control),
                colors = BulletinIconButtonDefaults.filledColors()
            ) { Icon(Icons.AutoMirrored.Filled.Send, composerActionLabel(isEditing, isBusy), Modifier.size(22.dp)) }
        }
        state.editError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun composerActionLabel(isEditing: Boolean, isBusy: Boolean): String = when {
    isEditing && isBusy -> "Saving..."
    isEditing -> "Save message"
    isBusy -> "Sending..."
    else -> "Send message"
}
