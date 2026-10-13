package com.jdrms.bulletin.app

import com.jdrms.bulletin.app.navigation.AppDestination
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId

internal class ListingMessagingActions(
    private val navigation: MainNavigationState,
    private val sendMessage: suspend (ListingMessageRequest) -> ConversationId?,
    private val findConversation: suspend (String, String) -> ConversationId?,
    private val hasConversation: suspend (String, String) -> Boolean
) {
    suspend fun send(listingId: String, sellerId: String, sellerName: String, content: String): Boolean {
        val conversationId = sendMessage(ListingMessageRequest(listingId, sellerId, sellerName, content))
            ?: return false
        navigation.pendingConversationId = conversationId
        navigation.currentDestination = AppDestination.MESSAGES
        return true
    }

    suspend fun seeChat(listingId: String, sellerId: String) {
        navigation.pendingConversationId = findConversation(listingId, sellerId)
        navigation.currentDestination = AppDestination.MESSAGES
    }

    suspend fun exists(listingId: String, sellerId: String): Boolean = hasConversation(listingId, sellerId)
}
