package com.jdrms.bulletin.domain.messages.application

import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import kotlin.time.Instant

/** One persisted incoming message that the viewer has not read and is not currently looking at. */
data class IncomingMessageAlert(
    val messageId: MessageId,
    val conversationId: ConversationId,
    val recipientId: SenderId,
    val senderName: String,
    val content: String,
    val createdAt: Instant,
    val senderId: SenderId? = null,
    val listingId: ListingReferenceId? = null
)

/** Platform port for surfacing alerts outside the app UI. Implementations must tolerate repeated calls. */
interface IncomingMessageNotifier {
    /** Shows or replaces the single alert for [IncomingMessageAlert.conversationId]. */
    fun show(alert: IncomingMessageAlert)

    /** Removes the alert for a conversation that was read, opened, or is no longer visible to the viewer. */
    fun clear(conversationId: ConversationId)

    /** Removes every message alert, e.g. on sign-out or account switch. */
    fun clearAll()
}

object NoOpIncomingMessageNotifier : IncomingMessageNotifier {
    override fun show(alert: IncomingMessageAlert) = Unit
    override fun clear(conversationId: ConversationId) = Unit
    override fun clearAll() = Unit
}
