package com.jdrms.bulletin.domain.messages.domain.model

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.service.MessagesPolicy
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

data class MessageEdit(
    val conversationId: ConversationId,
    val messageId: MessageId,
    val content: String,
    val expectedEditedAt: Instant?
)

enum class MessageEditFailure(val description: String) {
    UNAVAILABLE("This message is unavailable for editing."),
    EXPIRED("Messages can only be edited within 15 minutes of sending."),
    REPORTED("This message is locked for moderation."),
    CONFLICT("This message changed elsewhere. Cancel and reopen editing to load the latest version."),
    INVALID("Enter a message between 1 and 1000 characters.")
}

class MessageEditException(val failure: MessageEditFailure) : IllegalStateException(failure.description)

/** The server repeats these checks using its own clock and all reports, including private reports. */
object MessageEditing {
    fun canEdit(message: Message, viewerId: SenderId?, now: Instant): Boolean =
        message.senderId == viewerId && message.deletedAt == null && message.isEditLocked != true &&
            now >= message.orderingTimestamp && now < message.orderingTimestamp + 15.minutes

    fun apply(message: Message, editor: SenderId, edit: MessageEdit, now: Instant): Result<Message> {
        val failure = when {
            !isAccessible(message, editor, edit) -> MessageEditFailure.UNAVAILABLE
            now < message.orderingTimestamp || now >= message.orderingTimestamp + 15.minutes ->
                MessageEditFailure.EXPIRED
            message.isEditLocked == true -> MessageEditFailure.REPORTED
            message.editedAt != edit.expectedEditedAt -> MessageEditFailure.CONFLICT
            MessagesPolicy().validateMessageContent(edit.content.trim()) is Result.Error -> MessageEditFailure.INVALID
            else -> null
        }
        if (failure != null) return Result.Error(MessageEditException(failure))
        val content = edit.content.trim()
        val editedAt = message.editedAt?.let { maxOf(now, it + 1.microseconds) } ?: now
        return Result.Success(
            if (content == message.content) message else message.copy(content = content, editedAt = editedAt)
        )
    }
    private fun isAccessible(message: Message, editor: SenderId, edit: MessageEdit): Boolean {
        val matchesRequest = message.conversationId == edit.conversationId && message.id == edit.messageId
        return matchesRequest && message.senderId == editor && message.deletedAt == null
    }
}
