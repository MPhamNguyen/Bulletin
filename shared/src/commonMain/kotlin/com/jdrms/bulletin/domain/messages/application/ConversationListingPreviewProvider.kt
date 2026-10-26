package com.jdrms.bulletin.domain.messages.application

import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId

/** Messages-owned port for the small listing image snapshot shown beside a conversation. */
data class ConversationListingPreview(
    val title: String,
    val formattedPrice: String,
    val imageUrl: String?
)

fun interface ConversationListingPreviewProvider {
    suspend fun preview(listingId: ListingReferenceId): ConversationListingPreview?
}

object EmptyConversationListingPreviewProvider : ConversationListingPreviewProvider {
    override suspend fun preview(listingId: ListingReferenceId): ConversationListingPreview? = null
}
