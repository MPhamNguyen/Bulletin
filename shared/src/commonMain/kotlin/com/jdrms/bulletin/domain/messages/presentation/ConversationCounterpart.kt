package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant
import com.jdrms.bulletin.domain.messages.domain.model.SenderId

internal fun Conversation.counterpart(viewerId: SenderId?): ConversationParticipant? =
    viewerId?.takeIf(::includes)?.let { viewer -> participants.firstOrNull { it.id != viewer } }

/** The participant whose photo an inbox row shows; read-only rows keep the initials placeholder. */
internal fun Conversation.avatarPeer(viewerId: SenderId?): ConversationParticipant? =
    counterpart(viewerId)?.takeUnless { isReadOnly }

internal fun ConversationParticipant.blockTarget() = BlockTarget(id, displayName)
