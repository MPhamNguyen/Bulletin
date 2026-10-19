package com.jdrms.bulletin.app

import com.jdrms.bulletin.app.navigation.AppDestination
import com.jdrms.bulletin.app.navigation.MutableMainNavigationState
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ListingMessagingActionsTest {
    @Test
    fun successfulSendOpensTheCreatedConversation() = runTest {
        val navigation = MutableMainNavigationState()
        val id = ConversationId("conversation-1")
        var request: ListingMessageRequest? = null
        val actions = ListingMessagingActions(
            navigation,
            sendMessage = {
                request = it
                id
            },
            findConversation = { _, _ -> null },
            hasConversation = { _, _ -> false }
        )

        assertTrue(actions.send("listing-1", "seller-1", "Seller", "Hello"))
        assertEquals(ListingMessageRequest("listing-1", "seller-1", "Seller", "Hello"), request)
        assertEquals(AppDestination.MESSAGES, navigation.currentDestination)
        assertEquals(id, navigation.pendingConversationId)
    }

    @Test
    fun failedSendKeepsTheCurrentDestination() = runTest {
        val navigation = MutableMainNavigationState()
        val actions = ListingMessagingActions(
            navigation,
            sendMessage = { null },
            findConversation = { _, _ -> null },
            hasConversation = { _, _ -> false }
        )

        assertFalse(actions.send("listing-1", "seller-1", "Seller", "Hello"))
        assertEquals(AppDestination.HOME, navigation.currentDestination)
        assertEquals(null, navigation.pendingConversationId)
    }

    @Test
    fun seeChatOpensTheMatchingConversationAndExistenceUsesTheSameListing() = runTest {
        val navigation = MutableMainNavigationState()
        val id = ConversationId("conversation-1")
        val actions = ListingMessagingActions(
            navigation,
            sendMessage = { null },
            findConversation = { listingId, sellerId ->
                id.takeIf { listingId == "listing-1" && sellerId == "seller-1" }
            },
            hasConversation = { listingId, sellerId -> listingId == "listing-1" && sellerId == "seller-1" }
        )

        assertTrue(actions.exists("listing-1", "seller-1"))
        assertFalse(actions.exists("listing-2", "seller-1"))
        actions.seeChat("listing-1", "seller-1")
        assertEquals(AppDestination.MESSAGES, navigation.currentDestination)
        assertEquals(id, navigation.pendingConversationId)
    }
}
