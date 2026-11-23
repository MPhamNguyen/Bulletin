package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.runtime.Composable
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.SenderId

@Composable
internal fun InboxConversationAvatar(conversation: Conversation, viewerId: SenderId?) {
    val peer = conversation.avatarPeer(viewerId)
    MessageParticipantPhoto(
        name = conversationTitle(conversation, viewerId),
        senderId = peer?.id,
        initialUrl = peer?.avatarUrl
    )
}
