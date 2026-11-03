package com.jdrms.bulletin.domain.listings.domain.service

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.SellerId

sealed class ListingValidationException(message: String) : IllegalArgumentException(message) {
    class TitleTooShort : ListingValidationException("Listing title must be at least 3 characters.")
    class DescriptionEmpty : ListingValidationException("Listing description cannot be empty.")
    class PriceNegative : ListingValidationException("Listing price cannot be negative.")
    class InvalidAttributes(val issues: List<ListingAttributeIssue>) :
        ListingValidationException(issues.firstOrNull()?.message ?: "Listing attributes are not valid.")
    class UnknownAttributes(val keys: Set<String>) :
        ListingValidationException("Unknown listing attributes: ${keys.sorted().joinToString()}.")
    class Unauthorized(message: String = "Only the owner can edit this listing.") : ListingValidationException(message)
}

class ListingValidationPolicy(
    private val attributePolicy: ListingAttributeValidationPolicy = ListingAttributeValidationPolicy()
) {
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
        val attributeIssues = attributePolicy.validate(listing.category, listing.attributes)
        return if (attributeIssues.isNotEmpty()) {
            Result.Error(ListingValidationException.InvalidAttributes(attributeIssues))
        } else {
            Result.Success(Unit)
        }
    }

    /**
     * New attributes may only use catalog keys. [existing] is what storage already holds: preserved entries must
     * match it exactly, so a normal write can neither introduce an unknown key nor alter one it cannot interpret.
     */
    fun validatePreservedAttributes(submitted: Listing, existing: Listing?): Result<Unit> {
        val stored = existing?.attributes?.preserved.orEmpty()
        val introduced = submitted.attributes.preserved.filter { (key, raw) -> stored[key] != raw }.keys
        return if (introduced.isEmpty()) {
            Result.Success(Unit)
        } else {
            Result.Error(ListingValidationException.UnknownAttributes(introduced))
        }
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
