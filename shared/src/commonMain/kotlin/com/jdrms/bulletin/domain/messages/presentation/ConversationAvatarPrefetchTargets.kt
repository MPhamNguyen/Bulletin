package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.SenderId

/** Matches the inbox avatar membership rules and limits background work to the first page. */
internal data class ConversationAvatarPrefetchTarget(val senderId: SenderId, val url: String?)

internal fun conversationAvatarPrefetchTargets(
    viewerId: SenderId?,
    conversations: List<Conversation>
): List<ConversationAvatarPrefetchTarget> {
    if (viewerId == null) return emptyList()
    return conversations.take(10).mapNotNull { conversation ->
        if (!conversation.includes(viewerId) || conversation.isReadOnly) return@mapNotNull null
        conversation.participants.singleOrNull { it.id != viewerId }?.let { peer ->
            ConversationAvatarPrefetchTarget(peer.id, peer.avatarUrl?.takeIf(String::isNotBlank))
        }
    }.distinctBy { it.senderId }
}
