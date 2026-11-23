package com.jdrms.bulletin.domain.messages.domain.model

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.service.MessageTimeline
import kotlin.jvm.JvmInline
import kotlin.time.Instant

@JvmInline
value class ConversationId(val value: String)

@JvmInline
value class MessageId(val value: String)

@JvmInline
value class SenderId(val value: String) {
    init {
        require(value.isNotBlank()) { "Sender ID cannot be blank." }
    }
}

@JvmInline
value class ListingReferenceId(val value: String) {
    init { require(value.isNotBlank()) { "Listing reference cannot be blank." } }
}

data class ConversationParticipant(
    val id: SenderId,
    val displayName: String,
    val lastReadAt: Instant? = null
)

data class Message(
    val id: MessageId,
    val conversationId: ConversationId,
    val senderId: SenderId?,
    val senderName: String,
    val content: String,
    val timestampMillis: Long = 0L,
    val isReported: Boolean = false,
    val preciseCreatedAt: Instant? = null,
    val editedAt: Instant? = null,
    val deletedAt: Instant? = null,
    /** Null means a realtime row omitted report metadata; the server always enforces private report locks. */
    val isEditLocked: Boolean? = false
) {
    companion object {
        const val MISSING_SENDER_NAME = "User Not Found"
    }

    val displaySenderName: String get() = if (senderId == null) MISSING_SENDER_NAME else senderName
    val orderingTimestamp: Instant get() = preciseCreatedAt ?: Instant.fromEpochMilliseconds(timestampMillis)
    init {
        require(content.isNotBlank()) { "Message content cannot be blank." }
        require(content.length <= 1000) { "Message cannot exceed 1000 characters." }
    }

    fun report(): Message = copy(isReported = true, isEditLocked = true)
}

class Conversation(
    val id: ConversationId,
    participants: List<ConversationParticipant>,
    val lastMessage: Message? = null,
    val updatedAtMillis: Long = 0L,
    val listingId: ListingReferenceId? = null,
    val historyVisibleAfter: Instant? = null,
    val isListingSold: Boolean = false
) {
    val participants: List<ConversationParticipant> = participants.toList()
    val participantNames: List<String> get() = participants.map { it.displayName }

    init {
        require(participants.isNotEmpty()) { "A conversation must have participants." }
        require(participants.map { it.id }.distinct().size == participants.size) { "Participants must be unique." }
        require(lastMessage == null || lastMessage.conversationId == id) { "Message belongs to another conversation." }
        require(lastMessage?.senderId == null || includes(lastMessage.senderId)) { "Sender must be a participant." }
    }

    fun includes(senderId: SenderId): Boolean = participants.any { it.id == senderId }

    val lockReason: ConversationLockReason? get() = when {
        participants.size < 2 || (lastMessage != null && lastMessage.senderId == null) ->
            ConversationLockReason.ACCOUNT_DELETED
        isListingSold -> ConversationLockReason.LISTING_SOLD
        else -> null
    }

    val isReadOnly: Boolean get() = lockReason != null

    fun authorizeSending(senderId: SenderId?): Result<Unit> = when {
        senderId == null || !includes(senderId) -> Result.Error(ConversationAccessException())
        isReadOnly -> Result.Error(ConversationReadOnlyException(checkNotNull(lockReason)))
        else -> Result.Success(Unit)
    }

    fun recordReadAt(readerId: SenderId, readAt: Instant): Result<Conversation> {
        if (!includes(readerId)) return Result.Error(ConversationAccessException())
        val participant = participants.first { it.id == readerId }
        if (participant.lastReadAt?.let { it >= readAt } == true) return Result.Success(this)
        return Result.Success(
            Conversation(
                id = id,
                participants = participants.map {
                    if (it.id == readerId) it.copy(lastReadAt = readAt) else it
                },
                lastMessage = lastMessage,
                updatedAtMillis = updatedAtMillis,
                listingId = listingId,
                historyVisibleAfter = historyVisibleAfter,
                isListingSold = isListingSold
            )
        )
    }

    fun canSee(message: Message): Boolean =
        message.conversationId == id &&
            (historyVisibleAfter == null || message.orderingTimestamp > historyVisibleAfter)

    fun authorizeHiding(viewerId: SenderId): Result<Unit> =
        if (includes(viewerId)) Result.Success(Unit) else Result.Error(ConversationAccessException())

    fun otherParticipantNames(viewerId: SenderId): List<String> {
        require(includes(viewerId)) { "Viewer must be a participant." }
        return participants.filterNot { it.id == viewerId }.map { it.displayName }
    }

    fun reconcileMessages(messages: List<Message>): Result<Conversation> {
        if (messages.any { it.conversationId != id || (it.senderId != null && !includes(it.senderId)) }) {
            return Result.Error(ConversationAccessException())
        }
        val latest = messages.maxWithOrNull(compareBy<Message> { it.timestampMillis }.thenBy { it.id.value })
        return if (latest == null) Result.Success(this) else recordMessage(latest)
    }

    fun recordMessage(message: Message): Result<Conversation> {
        if (message.conversationId != id || (message.senderId != null && !includes(message.senderId))) {
            return Result.Error(ConversationAccessException())
        }
        val current = lastMessage
        val resolved = if (current?.id == message.id) {
            MessageTimeline.merge(listOf(current), listOf(message)).single()
        } else {
            message
        }
        val isNewer = current == null || message.timestampMillis > current.timestampMillis ||
            (message.timestampMillis == current.timestampMillis && message.id.value >= current.id.value)
        if (!isNewer) return Result.Success(this)

        return Result.Success(
            Conversation(
                id = id,
                participants = participants,
                lastMessage = resolved,
                updatedAtMillis = maxOf(updatedAtMillis, message.timestampMillis),
                listingId = listingId,
                historyVisibleAfter = historyVisibleAfter,
                isListingSold = isListingSold
            )
        )
    }
}

class ConversationAccessException : IllegalStateException("Conversation is unavailable for this user.")
