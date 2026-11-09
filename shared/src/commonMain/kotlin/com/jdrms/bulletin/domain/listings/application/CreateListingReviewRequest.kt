package com.jdrms.bulletin.domain.listings.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.SellerId

interface ListingReviewRequestCreator {
    suspend fun create(listing: Listing, buyerId: String): Result<Unit>
    suspend fun cancelForRestoredListing(listingId: ListingId, sellerId: SellerId): Result<Unit>
}

class CreateListingReviewRequest(private val creator: ListingReviewRequestCreator) {
    suspend operator fun invoke(listing: Listing, buyerId: String): Result<Unit> =
        creator.create(listing, buyerId)
}
