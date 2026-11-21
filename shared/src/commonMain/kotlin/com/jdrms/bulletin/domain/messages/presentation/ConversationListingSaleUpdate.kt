package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.application.ConversationListingPreview
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId

/** One pending optimistic update; its instance also identifies the request across session resets. */
class ConversationListingSaleUpdate(
    val listingId: String,
    val previousPreview: ConversationListingPreview,
    val optimisticPreview: ConversationListingPreview,
    val conversationId: ConversationId?
)
