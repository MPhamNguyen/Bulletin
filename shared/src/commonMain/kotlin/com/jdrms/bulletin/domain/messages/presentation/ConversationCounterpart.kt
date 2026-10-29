package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant
import com.jdrms.bulletin.domain.messages.domain.model.SenderId

internal fun Conversation.counterpart(viewerId: SenderId?): ConversationParticipant? =
    viewerId?.let { viewer -> participants.firstOrNull { it.id != viewer } }

internal fun ConversationParticipant.blockTarget() = BlockTarget(id, displayName)
