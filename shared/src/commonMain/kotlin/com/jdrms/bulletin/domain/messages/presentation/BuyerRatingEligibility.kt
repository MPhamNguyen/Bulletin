package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.application.ConversationListingPreview
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant
import com.jdrms.bulletin.domain.messages.domain.model.SenderId

/** Returns the seller's display name when the viewer is the chosen buyer and should be asked to rate them. */
internal fun buyerRatingPromptSeller(
    preview: ConversationListingPreview?,
    viewerId: SenderId?,
    seller: ConversationParticipant?
): String? {
    if (preview?.isSold != true || viewerId == null || seller == null) return null
    if (preview.soldToBuyerId != viewerId.value || preview.sellerId != seller.id.value) return null
    if (preview.sellerId == viewerId.value) return null
    return seller.displayName
}
