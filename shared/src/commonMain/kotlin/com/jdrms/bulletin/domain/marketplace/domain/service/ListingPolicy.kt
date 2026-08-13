package com.jdrms.bulletin.domain.marketplace.domain.service

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing

class ListingPolicy {
    fun validateListing(listing: Listing): Result<Unit> {
        if (listing.title.isBlank() || listing.title.length < 3) {
            return Result.Error(IllegalArgumentException("Listing title must be at least 3 characters long."))
        }
        if (listing.description.isBlank()) {
            return Result.Error(IllegalArgumentException("Listing description cannot be empty."))
        }
        if (listing.price.amount < 0.0) {
            return Result.Error(IllegalArgumentException("Listing price cannot be negative."))
        }
        return Result.Success(Unit)
    }
}
