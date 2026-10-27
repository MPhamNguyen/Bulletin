package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.repository.ListingsRepository
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.repository.MarketplaceRepository
import com.jdrms.bulletin.domain.messages.application.ConversationListingPreview
import com.jdrms.bulletin.domain.messages.application.ConversationListingPreviewProvider
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId

/** Translates marketplace listing data into the small preview contract owned by messages. */
class MarketplaceConversationListingPreviewProvider(
    private val listingsRepository: ListingsRepository,
    private val marketplaceRepository: MarketplaceRepository
) : ConversationListingPreviewProvider {
    override suspend fun preview(listingId: ListingReferenceId): ConversationListingPreview? {
        val id = listingId.value.removePrefix(LISTING_ID_PREFIX)
        val listing = listingsRepository.getListing(ListingId(id))
        if (listing != null) {
            return ConversationListingPreview(
                title = listing.title,
                formattedPrice = listing.price.formatted,
                imageUrl = listing.images.firstOrNull()
            )
        }
        val item = marketplaceRepository.getItem(MarketplaceItemId(id)) ?: return null
        return ConversationListingPreview(
            title = item.title,
            formattedPrice = item.price.formatted,
            imageUrl = item.photos.firstOrNull()
        )
    }

    private companion object {
        const val LISTING_ID_PREFIX = "listing:"
    }
}
