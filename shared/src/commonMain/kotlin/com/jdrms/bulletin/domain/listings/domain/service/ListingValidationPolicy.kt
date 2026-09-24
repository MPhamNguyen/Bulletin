package com.jdrms.bulletin.domain.listings.domain.service

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.domain.model.Listing

sealed class ListingValidationException(message: String) : IllegalArgumentException(message) {
    class TitleTooShort : ListingValidationException("Listing title must be at least 3 characters.")
    class DescriptionEmpty : ListingValidationException("Listing description cannot be empty.")
    class PriceNegative : ListingValidationException("Listing price cannot be negative.")
}

class ListingValidationPolicy {
    fun validateListing(listing: Listing): Result<Unit> {
        if (listing.title.isBlank() || listing.title.length < 3) {
            return Result.Error(ListingValidationException.TitleTooShort())
        }
        if (listing.description.isBlank()) {
            return Result.Error(ListingValidationException.DescriptionEmpty())
        }
        if (listing.price.amount < 0.0) {
            return Result.Error(ListingValidationException.PriceNegative())
        }
        return Result.Success(Unit)
    }
}
