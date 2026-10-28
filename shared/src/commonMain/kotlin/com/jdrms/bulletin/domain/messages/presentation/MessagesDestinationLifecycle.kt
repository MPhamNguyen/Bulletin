package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import kotlinx.coroutines.flow.MutableStateFlow

internal class MessagesDestinationLifecycle(
    private val inbox: InboxStateOwner,
    private val state: MutableStateFlow<MessagesUiState>,
    private val listingPreviewLoader: ConversationListingPreviewLoader,
    private val reloadSelectedConversation: (ConversationId) -> Unit,
    private val observeConversation: (ConversationId) -> Unit,
    private val onInactive: () -> Unit
) {
    var isActive = false
        private set

    fun onActiveChanged(isActive: Boolean) {
        if (this.isActive == isActive) return
        this.isActive = isActive
        if (!isActive) {
            onInactive()
            listingPreviewLoader.cancel()
            return
        }
        listingPreviewLoader.load(inbox.state.value.conversations)
        inbox.refresh()
        state.value.selectedConversationId?.let { conversationId ->
            reloadSelectedConversation(conversationId)
            observeConversation(conversationId)
        }
    }
}
