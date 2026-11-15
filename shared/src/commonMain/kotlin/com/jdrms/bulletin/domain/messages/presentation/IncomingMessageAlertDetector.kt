package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.application.IncomingMessageAlert
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import kotlin.time.Instant

internal data class AlertDetection(
    val alerts: List<IncomingMessageAlert> = emptyList(),
    val cleared: List<ConversationId> = emptyList(),
    val clearAll: Boolean = false
)

/**
 * Turns successive inbox snapshots into one alert per newly persisted incoming message.
 *
 * The first successful snapshot for an identity is a baseline: messages already in the inbox are never alerted.
 * Deduplication is per conversation by latest message ID, so refresh bursts, read-watermark changes, and
 * edits of an already-seen message cannot alert twice. The inbox exposes only each conversation's latest
 * message, so several messages arriving within one refresh collapse into one alert for the newest.
 *
 * A message is new when its ordering timestamp advances, or when it ties the stored timestamp but has a different
 * ID (two messages can share a timestamp). A strictly older timestamp is an inbox regression and is ignored.
 *
 * Notifications can outlive the process and the previous account's message text may still be visible, so the
 * first snapshot after construction and every viewer identity change (including signed out to signed in)
 * report [AlertDetection.clearAll], independent of what this instance has recorded.
 */
internal class IncomingMessageAlertDetector {
    private data class Seen(val id: MessageId, val at: Instant)

    private var started = false
    private var viewerId: SenderId? = null
    private var seeded = false
    private val seen = mutableMapOf<ConversationId, Seen>()
    private val alerted = mutableSetOf<ConversationId>()

    fun detect(state: InboxState, suppressed: ConversationId?): AlertDetection {
        val viewer = state.viewerId
        if (!started || viewer != viewerId) {
            started = true
            return resetForViewer(state, viewer)
        }
        if (viewer == null) return AlertDetection()
        if (!seeded) {
            seedIfReady(state, viewer)
            return AlertDetection()
        }
        val alerts = mutableListOf<IncomingMessageAlert>()
        val cleared = mutableListOf<ConversationId>()
        for (conversation in state.conversations) {
            val last = conversation.lastMessage ?: continue
            val isNewer = recordIfNewer(conversation.id, last)
            val unread = conversation.hasUnreadIncoming(viewer, last)
            when {
                !unread || conversation.id == suppressed ->
                    if (alerted.remove(conversation.id)) cleared += conversation.id
                isNewer -> {
                    alerted += conversation.id
                    alerts += last.toAlert(viewer, conversation.listingId)
                }
            }
        }
        return AlertDetection(alerts, cleared)
    }

    /** Records [last] as the latest seen message and reports whether it is new (see class docs). */
    private fun recordIfNewer(id: ConversationId, last: Message): Boolean {
        val previous = seen[id]
        val isNewer = previous == null ||
            last.orderingTimestamp > previous.at ||
            (last.orderingTimestamp == previous.at && last.id != previous.id)
        if (isNewer) seen[id] = Seen(last.id, last.orderingTimestamp)
        return isNewer
    }

    private fun resetForViewer(state: InboxState, viewer: SenderId?): AlertDetection {
        viewerId = viewer
        seeded = false
        seen.clear()
        alerted.clear()
        if (viewer != null) seedIfReady(state, viewer)
        return AlertDetection(clearAll = true)
    }

    private fun seedIfReady(state: InboxState, viewer: SenderId) {
        if (state.isLoading || state.errorMessage != null || state.viewerId != viewer) return
        state.conversations.forEach { conversation ->
            conversation.lastMessage?.let { seen[conversation.id] = Seen(it.id, it.orderingTimestamp) }
        }
        seeded = true
    }

    private fun Conversation.hasUnreadIncoming(viewer: SenderId, last: Message): Boolean {
        if (last.senderId == viewer || last.deletedAt != null) return false
        val readAt = participants.firstOrNull { it.id == viewer }?.lastReadAt
        return readAt == null || readAt < last.orderingTimestamp
    }

    private fun Message.toAlert(
        recipient: SenderId,
        listing: ListingReferenceId?
    ) = IncomingMessageAlert(
        messageId = id,
        conversationId = conversationId,
        recipientId = recipient,
        senderName = senderName,
        content = content,
        createdAt = orderingTimestamp,
        senderId = senderId,
        listingId = listing
    )
}
