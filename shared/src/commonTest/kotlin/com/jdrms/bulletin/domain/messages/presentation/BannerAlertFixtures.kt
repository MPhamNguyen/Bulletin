package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.application.IncomingMessageAlert
import com.jdrms.bulletin.domain.messages.application.IncomingMessageNotifier
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import kotlin.time.Instant

internal val bannerViewer = SenderId("me")
internal val bannerSender = SenderId("other")

internal fun bannerAlert(
    messageId: String,
    conversation: String = "thread",
    recipient: SenderId = bannerViewer,
    content: String = "hi $messageId"
) = IncomingMessageAlert(
    messageId = MessageId(messageId),
    conversationId = ConversationId(conversation),
    recipientId = recipient,
    senderName = "Other",
    content = content,
    createdAt = Instant.fromEpochMilliseconds(1_000)
)

internal fun bannerInbox(lastMessageId: String, at: Long) = InboxState(
    viewerId = bannerViewer,
    conversations = listOf(
        Conversation(
            id = ConversationId("thread"),
            participants = listOf(
                ConversationParticipant(bannerViewer, "Me"),
                ConversationParticipant(bannerSender, "Other")
            ),
            lastMessage = Message(
                MessageId(lastMessageId),
                ConversationId("thread"),
                bannerSender,
                "Other",
                "hi $lastMessageId",
                at
            )
        )
    ),
    isLoading = false
)

internal class RecordingNotifier : IncomingMessageNotifier {
    val shown = mutableListOf<IncomingMessageAlert>()
    override fun show(alert: IncomingMessageAlert) {
        shown += alert
    }
    override fun clear(conversationId: ConversationId) = Unit
    override fun clearAll() = Unit
}
