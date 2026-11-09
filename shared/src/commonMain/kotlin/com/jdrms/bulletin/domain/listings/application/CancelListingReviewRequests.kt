package com.jdrms.bulletin.domain.listings.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.SellerId

class CancelListingReviewRequests(private val creator: ListingReviewRequestCreator) {
    suspend operator fun invoke(listingId: ListingId, sellerId: SellerId): Result<Unit> =
        creator.cancelForRestoredListing(listingId, sellerId)
}
