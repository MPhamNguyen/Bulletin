package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.listings.application.ListingReviewRequestCreator
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequest
import com.jdrms.bulletin.domain.marketplace.domain.repository.ReviewRequestRepository

internal class ListingMarketplaceReviewRequestCreator(
    private val repository: () -> ReviewRequestRepository
) : ListingReviewRequestCreator {
    override suspend fun create(listing: Listing, buyerId: String) = repository().createIfMissing(
        ReviewRequest(
            listingId = MarketplaceItemId(listing.id.value),
            buyerId = buyerId,
            sellerId = listing.sellerId.value,
            listingTitle = listing.title
        )
    )

    override suspend fun cancelForRestoredListing(listingId: ListingId, sellerId: SellerId) =
        repository().cancelForRestoredListing(listingId.value, sellerId.value)
}
