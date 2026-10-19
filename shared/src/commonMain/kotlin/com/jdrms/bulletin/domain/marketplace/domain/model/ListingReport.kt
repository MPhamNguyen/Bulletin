package com.jdrms.bulletin.domain.marketplace.domain.model

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.reporting.domain.model.ReportReason
import com.jdrms.bulletin.domain.reporting.domain.model.ReportType

@JvmInline
value class MarketplaceReporterId(val value: String) {
    init {
        require(value.isNotBlank()) { "Reporter ID is required." }
    }
}

data class ListingReport(
    val listingId: MarketplaceItemId,
    val listingSellerId: String,
    val reporterId: MarketplaceReporterId,
    val reason: ReportReason,
    val description: String
) {
    init {
        require(reason.appliesTo(ReportType.LISTING)) { "Report reason is not valid for listings." }
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
            reason: ReportReason,
            description: String
        ): Result<ListingReport> {
            if (!reason.appliesTo(ReportType.LISTING)) {
                return Result.Error(IllegalArgumentException("Report reason is not valid for listings."))
            }
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
