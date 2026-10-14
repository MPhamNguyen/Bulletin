package com.jdrms.bulletin.app

import com.jdrms.bulletin.app.di.AppContainer
import com.jdrms.bulletin.app.navigation.AppDestination
import com.jdrms.bulletin.app.navigation.MainNavigationState
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.presentation.MessagesViewModel

internal data class ListingMessageSendResult(
    val sent: Boolean,
    val conversationId: ConversationId?,
    val navigationState: MainNavigationState
)

internal data class ListingMessageRequest(
    val listingId: String,
    val sellerId: String,
    val sellerName: String,
    val content: String
)

internal fun openSentListingConversation(
    result: ListingMessageSendResult,
    messagesViewModel: MessagesViewModel
) {
    if (result.sent) {
        result.conversationId?.let(messagesViewModel.openConversation)
    }
}

internal suspend fun sendListingMessage(
    container: AppContainer,
    navigationState: MainNavigationState,
    request: ListingMessageRequest
): ListingMessageSendResult {
    val conversationId = container.messageSeller(
        ListingReferenceId(request.listingId.removePrefix("listing:")),
        ConversationParticipant(SenderId(request.sellerId), request.sellerName),
        request.content
    ).getOrNull()?.id
    return ListingMessageSendResult(
        sent = conversationId != null,
        conversationId = conversationId,
        navigationState = if (conversationId != null) {
            navigationState.selectBottomNavigationDestination(AppDestination.MESSAGES)
        } else {
            navigationState
        }
    )
}

internal suspend fun hasListingConversation(
    container: AppContainer,
    listingId: String,
    sellerId: String
): Boolean {
    return container.hasListingConversation(
        ListingReferenceId(listingId.removePrefix("listing:")),
        SenderId(sellerId)
    ).getOrNull() == true
}

private suspend fun findListingConversation(
    container: AppContainer,
    listingId: String,
    sellerId: String
): ConversationId? {
    val listingReference = ListingReferenceId(listingId.removePrefix("listing:"))
    val seller = SenderId(sellerId)
    return container.getConversations().getOrNull()?.conversations?.firstOrNull { conversation ->
        conversation.listingId == listingReference && conversation.includes(seller)
    }?.id
}

internal suspend fun openListingConversation(
    container: AppContainer,
    messagesViewModel: MessagesViewModel,
    listingId: String,
    sellerId: String
) {
    findListingConversation(container, listingId, sellerId)?.let(messagesViewModel.openConversation)
}
