package com.jdrms.bulletin.domain.marketplace.domain.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing

/** Reads uncached listing state so review eligibility is checked against the latest server value. */
interface MarketplaceReviewListingReader {
    suspend fun getListingForReview(listingID: String): Result<Listing>
}
