package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.EditMessage
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageEdit
import com.jdrms.bulletin.domain.messages.domain.model.MessageEditException
import com.jdrms.bulletin.domain.messages.domain.model.MessageEditing
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.service.MessageTimeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant

data class MessageEditSession(val original: Message, val viewerId: SenderId, val savedInput: String)

sealed interface MessageEditState {
    data object Composing : MessageEditState
    data class Editing(val session: MessageEditSession) : MessageEditState
    data class Saving(val session: MessageEditSession) : MessageEditState
}

/** Owns editing in the shared composer, including preservation of the unsent draft. */
internal class MessageEditingActions(
    private val inbox: InboxStateOwner,
    private val state: MutableStateFlow<MessagesUiState>,
    private val editMessage: EditMessage?,
    private val scope: CoroutineScope,
    private val now: () -> Instant = { Clock.System.now() }
) {
    fun begin(messageId: MessageId) {
        val snapshot = state.value
        val viewerId = inbox.state.value.viewerId ?: return
        val message = snapshot.currentMessages.firstOrNull { it.id == messageId } ?: return
        val busy = snapshot.isSending || snapshot.editState is MessageEditState.Saving
        if (editMessage == null || busy || !MessageEditing.canEdit(message, viewerId, now())
        ) {
            return
        }
        val previous = (snapshot.editState as? MessageEditState.Editing)?.session?.savedInput ?: snapshot.messageInput
        state.update {
            it.copy(
                editState = MessageEditState.Editing(MessageEditSession(message, viewerId, previous)),
                messageInput = message.content,
                editError = null
            )
        }
    }

    fun cancel() {
        val editing = state.value.editState as? MessageEditState.Editing ?: return
        state.update {
            it.copy(
                editState = MessageEditState.Composing,
                messageInput = editing.session.savedInput,
                editError = null
            )
        }
    }

    fun save() {
        val snapshot = state.value
        val editing = snapshot.editState as? MessageEditState.Editing ?: return
        val action = editMessage ?: return
        val session = editing.session
        if (inbox.state.value.viewerId != session.viewerId) return
        state.update { it.copy(editState = MessageEditState.Saving(session), editError = null) }
        scope.launch {
            val original = session.original
            val result =
                action(MessageEdit(original.conversationId, original.id, snapshot.messageInput, original.editedAt))
            inbox.synchronizeIdentity()
            if (inbox.state.value.viewerId != session.viewerId ||
                state.value.editState != MessageEditState.Saving(session)
            ) {
                return@launch
            }
            complete(session, result)
        }
    }

    private fun complete(session: MessageEditSession, result: Result<Message>) {
        when (result) {
            is Result.Error -> state.update {
                it.copy(
                    editState = MessageEditState.Editing(session),
                    editError = (result.exception as? MessageEditException)?.failure?.description
                        ?: "Unable to save changes. Check your connection and try again."
                )
            }
            is Result.Success -> {
                val saved = result.data.copy(senderName = session.original.senderName)
                inbox.recordMessage(saved)
                state.update {
                    it.copy(
                        currentMessages = MessageTimeline.merge(it.currentMessages, listOf(saved)),
                        editState = MessageEditState.Composing,
                        messageInput = session.savedInput,
                        editError = null
                    )
                }
            }
        }
    }
}

internal val MessagesUiState.unsentInput: String
    get() = when (val editing = editState) {
        MessageEditState.Composing -> messageInput
        is MessageEditState.Editing -> editing.session.savedInput
        is MessageEditState.Saving -> editing.session.savedInput
    }
