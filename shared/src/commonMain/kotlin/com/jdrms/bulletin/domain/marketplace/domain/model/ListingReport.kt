package com.jdrms.bulletin.domain.marketplace.domain.model

import com.jdrms.bulletin.core.common.Result

@JvmInline
value class MarketplaceReporterId(val value: String) {
    init {
        require(value.isNotBlank()) { "Reporter ID is required." }
    }
}

enum class ListingReportReason(val label: String) {
    PROHIBITED_ITEM("Prohibited item"),
    MISLEADING_LISTING("Misleading listing"),
    SCAM_OR_FRAUD("Scam or fraud"),
    DUPLICATE_LISTING("Duplicate listing"),
    OTHER("Other")
}

data class ListingReport(
    val listingId: MarketplaceItemId,
    val listingSellerId: String,
    val reporterId: MarketplaceReporterId,
    val reason: ListingReportReason,
    val description: String
) {
    init {
        require(listingId.value.isNotBlank()) { "Listing ID is required." }
        require(listingSellerId.isNotBlank()) { "Listing seller ID is required." }
        require(reporterId.value != listingSellerId) { "You cannot report your own listing." }
        require(description.length <= MAX_DESCRIPTION_LENGTH) {
            "Description must be $MAX_DESCRIPTION_LENGTH characters or fewer."
        }
        require(description == description.trim()) { "Report description must be trimmed." }
    }

    companion object {
        const val MAX_DESCRIPTION_LENGTH = 1_000

        fun create(
            listingId: MarketplaceItemId,
            listingSellerId: String,
            reporterId: MarketplaceReporterId,
            reason: ListingReportReason,
            description: String
        ): Result<ListingReport> {
            val normalizedDescription = description.trim()
            if (listingId.value.isBlank() || listingSellerId.isBlank()) {
                return Result.Error(IllegalArgumentException("Listing identity is required."))
            }
            if (reporterId.value == listingSellerId) {
                return Result.Error(IllegalArgumentException("You cannot report your own listing."))
            }
            if (normalizedDescription.length > MAX_DESCRIPTION_LENGTH) {
                return Result.Error(
                    IllegalArgumentException("Description must be $MAX_DESCRIPTION_LENGTH characters or fewer.")
                )
            }
            return Result.Success(
                ListingReport(listingId, listingSellerId, reporterId, reason, normalizedDescription)
            )
        }
    }
}
