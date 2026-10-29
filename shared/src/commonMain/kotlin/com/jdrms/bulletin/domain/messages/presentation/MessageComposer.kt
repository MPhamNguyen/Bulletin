package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinButtonDefaults
import com.jdrms.bulletin.core.designsystem.BulletinTextField

@Composable
internal fun MessageComposer(state: MessagesUiState, viewModel: MessagesViewModel) {
    val isEditing = state.editState != MessageEditState.Composing
    val isBusy = state.isSending || state.editState is MessageEditState.Saving
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        if (isEditing) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Editing message", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = viewModel.cancelEditing, enabled = !isBusy) { Text("Cancel") }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            BulletinTextField(
                value = state.messageInput,
                onValueChange = viewModel::onMessageInputChanged,
                modifier = Modifier.weight(1f),
                options = {
                    this.placeholder = { Text(if (isEditing) "Edit your message..." else "Type a message...") }
                    this.enabled = !isBusy
                    this.maxLines = 4
                    this.isError = state.editError != null
                }
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = viewModel::sendCurrentMessage,
                enabled = !isBusy && !state.isLoadingMessages && state.messageInput.isNotBlank(),
                colors = BulletinButtonDefaults.buttonColors()
            ) { Text(composerActionLabel(isEditing, isBusy)) }
        }
        state.editError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun composerActionLabel(isEditing: Boolean, isBusy: Boolean): String = when {
    isEditing && isBusy -> "Saving..."
    isEditing -> "Save"
    isBusy -> "Sending..."
    else -> "Send"
}
