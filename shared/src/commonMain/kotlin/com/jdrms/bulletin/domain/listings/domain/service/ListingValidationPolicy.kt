package com.jdrms.bulletin.domain.listings.domain.service

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.SellerId

sealed class ListingValidationException(message: String) : IllegalArgumentException(message) {
    class TitleTooShort : ListingValidationException("Listing title must be at least 3 characters.")
    class DescriptionEmpty : ListingValidationException("Listing description cannot be empty.")
    class PriceNegative : ListingValidationException("Listing price cannot be negative.")
    class Unauthorized(message: String = "Only the owner can edit this listing.") : ListingValidationException(message)
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

    fun validateOwnership(
        listing: Listing,
        editorSellerId: SellerId,
        action: String = "edit"
    ): Result<Unit> {
        if (!listing.isOwnedBy(editorSellerId)) {
            return Result.Error(
                ListingValidationException.Unauthorized("Only the owner can $action this listing.")
            )
        }
        return Result.Success(Unit)
    }
}
