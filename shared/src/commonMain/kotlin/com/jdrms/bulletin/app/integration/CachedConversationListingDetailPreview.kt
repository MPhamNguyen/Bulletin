package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceListingPreview
import com.jdrms.bulletin.domain.messages.application.ConversationListingPreview
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.PublicProfilePhotoRepository

/** Reuses the listing seller's cached photo without fetching a photo just for the loading preview. */
internal suspend fun ConversationListingPreview.toCachedMarketplaceDetailPreview(
    listingId: String,
    photos: PublicProfilePhotoRepository
): MarketplaceListingPreview? {
    val preview = toMarketplaceDetailPreview(listingId) ?: return null
    return preview.copy(sellerAvatarUrl = photos.getCachedAvatarUrl(UserId(preview.sellerId)))
}
