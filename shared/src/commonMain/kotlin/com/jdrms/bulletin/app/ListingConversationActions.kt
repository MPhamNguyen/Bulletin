package com.jdrms.bulletin.app

import com.jdrms.bulletin.app.di.MessagesModule
import com.jdrms.bulletin.app.navigation.AppDestination
import com.jdrms.bulletin.app.navigation.MutableMainNavigationState
import com.jdrms.bulletin.domain.messages.presentation.MessagesViewModel

internal class ListingConversationActions(
    private val messagesModule: MessagesModule,
    private val navigation: MutableMainNavigationState,
    private val messages: MessagesViewModel
) {
    suspend fun messageSeller(
        listingId: String,
        sellerId: String,
        sellerName: String,
        content: String
    ): Boolean {
        val result = sendListingMessage(
            messagesModule,
            navigation.snapshot(),
            ListingMessageRequest(listingId, sellerId, sellerName, content)
        )
        openSentListingConversation(result, messages)
        navigation.apply(result.navigationState)
        return result.sent
    }

    suspend fun hasExistingConversation(listingId: String, sellerId: String): Boolean =
        hasListingConversation(messagesModule, listingId, sellerId)

    suspend fun seeChat(listingId: String, sellerId: String) {
        openListingConversation(messagesModule, messages, listingId, sellerId)
        navigation.apply(navigation.snapshot().selectBottomNavigationDestination(AppDestination.MESSAGES))
    }
}
