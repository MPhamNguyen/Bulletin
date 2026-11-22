package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.application.GetConversations
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.SenderId

/** Uses the inbox row's avatar rule and limits background work to the first page. */
internal data class ConversationAvatarPrefetchTarget(val senderId: SenderId, val url: String?)

internal fun conversationAvatarPrefetchTargets(
    viewerId: SenderId?,
    conversations: List<Conversation>
): List<ConversationAvatarPrefetchTarget> =
    conversations.take(GetConversations.PAGE_SIZE).mapNotNull { it.avatarPeer(viewerId) }
        .map { peer -> ConversationAvatarPrefetchTarget(peer.id, peer.avatarUrl?.takeIf(String::isNotBlank)) }
        .distinctBy { it.senderId }
