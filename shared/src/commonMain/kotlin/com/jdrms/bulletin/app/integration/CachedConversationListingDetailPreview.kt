package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceListingPreview
import com.jdrms.bulletin.domain.messages.application.ConversationListingPreview
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.PublicProfilePhotoRepository

/** Reuses the listing seller's cached photo without fetching a photo just for the loading preview. */
internal suspend fun ConversationListingPreview.toCachedMarketplaceDetailPreview(
    listingId: String,
    photos: PublicProfilePhotoRepository,
    conversations: List<Conversation> = emptyList()
): MarketplaceListingPreview? {
    val preview = toMarketplaceDetailPreview(listingId) ?: return null
    val knownAvatar = conversations.asSequence().flatMap { it.participants.asSequence() }
        .firstOrNull { it.id.value == preview.sellerId && !it.avatarUrl.isNullOrBlank() }?.avatarUrl
    return preview.copy(sellerAvatarUrl = knownAvatar ?: photos.getCachedAvatarUrl(UserId(preview.sellerId)))
}
