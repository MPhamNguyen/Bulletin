package com.jdrms.bulletin.domain.messages.infrastructure.repository

import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationInboxDto

internal interface SupabaseInboxTable {
    suspend fun getInboxConversations(userId: String): List<SupabaseConversationInboxDto>
    suspend fun getInboxPage(userId: String, offset: Int, limit: Int): List<SupabaseConversationInboxDto> =
        getInboxConversations(userId).drop(offset).take(limit)
    suspend fun getInboxConversation(userId: String, conversationId: String): SupabaseConversationInboxDto? =
        getInboxConversations(userId).firstOrNull { it.id == conversationId }
}
