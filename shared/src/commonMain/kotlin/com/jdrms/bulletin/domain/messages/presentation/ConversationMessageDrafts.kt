package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** In-memory drafts for the current messaging session; the active draft lives in composer state. */
internal class ConversationMessageDrafts(private val state: MutableStateFlow<MessagesUiState>) {
    private val drafts = mutableMapOf<ConversationId, String>()
    var session: Any = Any()
        private set

    fun reset() {
        drafts.clear()
        session = Any()
    }

    fun restore(conversationId: ConversationId?): String {
        val current = state.value
        current.selectedConversationId?.let { id ->
            if (current.unsentInput.isEmpty()) drafts.remove(id) else drafts[id] = current.unsentInput
        }
        return drafts.remove(conversationId).orEmpty()
    }

    fun inputChanged(input: String) {
        state.update {
            val conversationId = it.selectedConversationId
            it.copy(
                messageInput = input,
                editError = null,
                errorMessage = null,
                statusMessage = null,
                sendStates = if (conversationId != null && it.sendErrorMessage != null &&
                    it.editState == MessageEditState.Composing
                ) {
                    it.sendStates - conversationId
                } else {
                    it.sendStates
                }
            )
        }
    }

    fun clearSent(conversationId: ConversationId, sentInput: String) {
        if (drafts[conversationId] == sentInput) drafts.remove(conversationId)
        state.update { current ->
            if (current.selectedConversationId == conversationId &&
                current.editState == MessageEditState.Composing && current.messageInput == sentInput
            ) {
                current.copy(messageInput = "")
            } else {
                current
            }
        }
    }
}
