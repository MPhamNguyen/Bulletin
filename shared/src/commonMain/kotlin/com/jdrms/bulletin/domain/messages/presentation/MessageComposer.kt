package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.BulletinFieldDecoration
import com.jdrms.bulletin.core.designsystem.BulletinFieldOptions
import com.jdrms.bulletin.core.designsystem.BulletinTextField
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

@Composable
internal fun MessageComposer(state: MessagesUiState, viewModel: MessagesViewModel) {
    val isEditing = state.editState != MessageEditState.Composing
    val isBusy = state.isSending || state.editState is MessageEditState.Saving || !state.canSendToCounterpart()
    Column(Modifier.fillMaxWidth().padding(horizontal = BulletinSpacing.m, vertical = BulletinSpacing.s)) {
        if (isEditing) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Editing message", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                BulletinButton(
                    onClick = viewModel.cancelEditing,
                    enabled = !isBusy,
                    variant = BulletinButtonVariant.Text
                ) { Text("Cancel") }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            BulletinTextField(
                value = state.messageInput,
                onValueChange = viewModel::onMessageInputChanged,
                modifier = Modifier.weight(1f),
                isError = state.editError != null,
                decoration = BulletinFieldDecoration(
                    placeholder = { Text(if (isEditing) "Edit your message..." else "Type a message...") }
                ),
                options = BulletinFieldOptions(
                    enabled = !isBusy,
                    maxLines = 4
                )
            )
            Spacer(Modifier.width(BulletinSpacing.s))
            BulletinButton(
                onClick = viewModel::sendCurrentMessage,
                enabled = !isBusy && !state.isLoadingMessages && state.messageInput.isNotBlank(),
                variant = BulletinButtonVariant.Primary
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
