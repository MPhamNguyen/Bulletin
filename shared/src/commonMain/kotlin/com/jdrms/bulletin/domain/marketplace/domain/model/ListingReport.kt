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
            val normalizedDescription = description.trim()
            val validationFailure = when {
                !reason.appliesTo(ReportType.LISTING) ->
                    IllegalArgumentException("Report reason is not valid for listings.")
                listingId.value.isBlank() || listingSellerId.isBlank() ->
                    IllegalArgumentException("Listing identity is required.")
                reporterId.value == listingSellerId ->
                    IllegalArgumentException("You cannot report your own listing.")
                normalizedDescription.length > MAX_DESCRIPTION_LENGTH ->
                    IllegalArgumentException("Description must be $MAX_DESCRIPTION_LENGTH characters or fewer.")
                else -> null
            }
            if (validationFailure != null) return Result.Error(validationFailure)
            return Result.Success(
                ListingReport(listingId, listingSellerId, reporterId, reason, normalizedDescription)
            )
        }
    }
}
